package com.cardiomood.multiuser.screen.entry

import com.cardiomood.multiuser.api.GroupInfo
import com.cardiomood.multiuser.mvp.BasePresenter
import com.jakewharton.rxrelay.BehaviorRelay
import com.jakewharton.rxrelay.SerializedRelay
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.concurrent.TimeUnit

class EntryPresenter(private val model: EntryModel, private val groupState: BehaviorRelay<GroupInfo?>) : BasePresenter<EntryView, EntryRouter>() {

    private val CODE_PATTERN = Regex("[a-z0-9]{4,6}", RegexOption.IGNORE_CASE)

    private val groupCodeStream = viewStream(BehaviorRelay.create<String>()) { view ->
        view.groupCodeInputStream.skip(1)
    }

    private val submitClicks = viewStream { view -> view.submitClicks }

    private val stateStream = SerializedRelay(BehaviorRelay.create<RequestState>(RequestState.NONE))

    private val buttonEnabledState = presenterStream {
        Observable.combineLatest(
                groupCodeStream.startWith(""), stateStream,
                { code, state -> code.matches(CODE_PATTERN) && !state.blocking }
        )
                .replay(1)
    }

    private val clearErrorStream = presenterStream {
        groupCodeStream.startWith("")
                .map { Unit }
                .publish()
    }

    private val dataStream = presenterStream {
        submitClicks.debounce(200, TimeUnit.MILLISECONDS)
                .withLatestFrom(
                        groupCodeStream.startWith("").filter { it.matches(CODE_PATTERN) },
                        { click, code -> code }
                )
                .switchMap {
                    model.getGroupByCode(it)
                            .map { GroupRequestResolution.Success(it) }
                            .cast(GroupRequestResolution::class.java)
                            .doOnSubscribe { stateStream.call(RequestState.PROGRESS) }
                            .onErrorReturn {
                                when (it) {
                                    is IOException -> GroupRequestResolution.NetworkError
                                    else -> GroupRequestResolution.NotFoundError
                                }
                            }
                            .doOnNext { stateStream.call(RequestState.COMPLETED) }
                            .subscribeOn(Schedulers.io())
                }
                .publish()
    }

    private val notFoundErrorStream =
            dataStream.ofType(GroupRequestResolution.NotFoundError::class.java).map { Unit }
    private val networkErrorStream =
            dataStream.ofType(GroupRequestResolution.NetworkError::class.java).map { Unit }
    private val successStream =
            dataStream.ofType(GroupRequestResolution.Success::class.java).map { it.data }

    override fun attachRouter(router: EntryRouter) {
        routerSubscription.addAll(
                successStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            groupState.call(it)
                            router.gotoMainScreen(it)
                            router.finish()
                        }
        )
        super.attachRouter(router)
    }

    override fun attachView(view: EntryView) {
        viewSubscription.addAll(
                networkErrorStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.showNetworkError),
                notFoundErrorStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.showNotFoundError),
                clearErrorStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.clearError),
                buttonEnabledState.observeOn(AndroidSchedulers.mainThread())
                        .subscribe(view.enableButton),
                stateStream.observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            when (it) {
                                RequestState.PROGRESS -> view.showProgress()
                                RequestState.COMPLETED -> view.hideProgress()
                                else -> {
                                    // do nothing
                                }
                            }
                        }
        )
        super.attachView(view)
    }

    enum class RequestState(val blocking: Boolean = false) {

        NONE(), PROGRESS(true), COMPLETED()

    }

    sealed class GroupRequestResolution {
        class Success(val data: GroupInfo) : GroupRequestResolution()
        object NotFoundError : GroupRequestResolution()
        object NetworkError : GroupRequestResolution()
    }
}
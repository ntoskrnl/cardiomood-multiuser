package ru.test.multydevicetest.ui;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.cardiomood.group.R;

/**
 * Created by Bes on 17.08.2016.
 */
public class OverviewPresentation extends Presentation {
    private final static String TAG = OverviewPresentation.class.getSimpleName();
    protected Context ctx;
    //protected final OverviewManager manager;
    public OverviewPresentation(Context outerContext, Display display) {
        super(outerContext, display);
        this.ctx = outerContext;
        //this.manager = new OverviewManager(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_overview);

        Button buttonInc = (Button) findViewById(R.id.button_inc);
        Button buttonDec = (Button) findViewById(R.id.button_dec);
        if(buttonInc != null) buttonInc.setVisibility(View.GONE);
        if(buttonInc != null) buttonDec.setVisibility(View.GONE);
    }
}

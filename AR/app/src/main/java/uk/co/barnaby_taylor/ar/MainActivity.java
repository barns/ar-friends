package uk.co.barnaby_taylor.ar;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;


public class MainActivity extends FragmentActivity {

    private MainFragment mainFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            mainFragment = new MainFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, mainFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            mainFragment = (MainFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }
//        mainFragment.onDestroy();
        getSupportFragmentManager().beginTransaction().remove(mainFragment);
        setContentView(R.layout.activity_main);

        FrameLayout arViewPane = (FrameLayout) findViewById(R.id.ar_view_pane);

        ArDisplayView arDisplay = new ArDisplayView(this, this);

//        arViewPane.addView(connect);
        arViewPane.addView(arDisplay);

        OverlayView arContent = new OverlayView(getApplicationContext());
        arViewPane.addView(arContent);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onDestroy() {
        super.onDestroy();
    }
}

package jaelyn.path;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final PathView pathView = (PathView) findViewById(R.id.pathview);
        findViewById(R.id.percent_30).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pathView.setScale(0);
            }
        });
        findViewById(R.id.percent_60).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pathView.setScale(1);
            }
        });
        findViewById(R.id.percent_100).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pathView.setScale(2);
            }
        });
    }
}

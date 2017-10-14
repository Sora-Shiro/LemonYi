/*
 * Copyright (C) 2017 Sora Shiro (https://github.com/Sora-Shiro)
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sorashiro.desktoppet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.sorashiro.desktoppet.permission.FloatWindowManager;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button   btnGenerate;
    private Button   btnRemove;
    private Button   btnAbout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        btnGenerate = (Button) findViewById(R.id.btnGenerate);
        btnRemove = (Button) findViewById(R.id.btnRemove);
        btnAbout = (Button) findViewById(R.id.btnAbout);

        btnGenerate.setOnClickListener(this);
        btnRemove.setOnClickListener(this);
        btnAbout.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnGenerate:
                FloatWindowManager.getInstance().applyOrShowFloatWindow(MainActivity.this);
                break;
            case R.id.btnRemove:
                FloatWindowManager.getInstance().dismissWindow(MainActivity.this);
                break;
            case R.id.btnAbout:
                Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(intent);
                break;
        }
    }


}

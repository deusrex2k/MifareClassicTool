/*
 * Copyright 2013 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


package com.lot38designs.cfsrfid.Activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.nfc.tech.MifareClassic;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.TextViewCompat;

import com.lot38designs.cfsrfid.Common;
import com.lot38designs.cfsrfid.MCReader;
import com.lot38designs.cfsrfid.R;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Write data to tag. The user can choose to write
 * a single block of data or to write a dump to a tag providing its keys
 * or to factory format a tag.
 * @author Gerhard Klostermeier
 */
public class Materials extends BasicActivity {


    private static final int CKM_WRITE_NEW_VALUE = 5;

    private EditText editTextPrinterIP;

    /**
     * Initialize the layout and some member variables.
     */
    // It is checked but the IDE don't get it.
    @SuppressWarnings({"unchecked"})
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materials);

        editTextPrinterIP = findViewById(R.id.editTextPrinterIP);

        Intent i = getIntent();
    }

    /**
     * Save important state data before this activity gets destroyed.
     * @param outState The state to put data into.
     */
    @Override
    protected void onSaveInstanceState (Bundle outState) {
        super.onSaveInstanceState(outState);
        //TODO: save printer IP

    }

    public void onPrinter(){
        String BASE_URL = "\"http://\" + editTextPrinterIP + \"/downloads/defData/material_database.json\"";

        //create list of suppliers
        //create list(s) or materials
        Retrofit retrofit = new Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl(BASE_URL)
            .build();

        parseJson();

    }

    public void onFile(){
        parseJson();
    }

    public void onCancelPressed(){
        finish();
    }

    private void parseJson(){


    }


    /**

     */
    @Override
    public void onActivityResult(int requestCode,
            int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        int ckmError = -1;

        switch(requestCode) {
        /*case FC_WRITE_DUMP:
            if (resultCode == Activity.RESULT_OK) {
                // Read dump and create keys.
                readDumpFromFile(data.getStringExtra(
                        FileChooser.EXTRA_CHOSEN_FILE));
            }
            break;
        case CKM_WRITE_DUMP:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                checkDumpAgainstTag();
            }
            break;
        case CKM_FACTORY_FORMAT:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                createFactoryFormattedDump();
            }
            break;
        case CKM_WRITE_BLOCK:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                // Write block.
                writeBlock();
            }
            break;
        case CKM_WRITE_NEW_VALUE:
            if (resultCode != Activity.RESULT_OK) {
                // Error.
                ckmError = resultCode;
            } else {
                // Write block.
                writeValueBlock();
            }
            break;*/

        }

        // Error handling for the return value of KeyMapCreator.
        // So far, only error nr. 4 needs to be handled.
        if (ckmError == 4) {// Error. Path from the calling intend was null.
            // (This is really strange and should not occur.)
            Toast.makeText(this, R.string.info_strange_error,
                Toast.LENGTH_LONG).show();
        }
    }


}

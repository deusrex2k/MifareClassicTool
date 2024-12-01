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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.pm.PackageInfoCompat;
import androidx.core.text.HtmlCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.lot38designs.cfsrfid.Common;
import com.lot38designs.cfsrfid.MCReader;
import com.lot38designs.cfsrfid.R;
import com.skydoves.colorpickerview.*;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;


/**
 * Main App entry point showing the main menu.
 * Some stuff about the App:
 * <ul>
 * <li>Error/Debug messages (Log.e()/Log.d()) are hard coded</li>
 * <li>This is my first App, so please by decent with me ;)</li>
 * </ul>
 * @author Gerhard Klostermeier
 */
public class MainMenu extends AppCompatActivity {

    private static final String LOG_TAG =
            MainMenu.class.getSimpleName();

    private final static int FILE_CHOOSER_DUMP_FILE = 1;
    private final static int FILE_CHOOSER_KEY_FILE = 2;
    private boolean mDonateDialogWasShown = false;
    private boolean mInfoExternalNfcDialogWasShown = false;
    private boolean mHasNoNfc = false;
    private Button mReadTag;
    private Button mWriteTag;
    private Button mKeyEditor;
    private Button mDumpEditor;
    private Intent mOldIntent = null;

    public final static String EXTRA_DUMP =
        "de.syss.MifareClassicTool.Activity.DUMP";

    private static final int FC_WRITE_DUMP = 1;
    private static final int CKM_WRITE_DUMP = 2;
    private static final int CKM_WRITE_BLOCK = 3;
    private static final int CKM_FACTORY_FORMAT = 4;
    private static final int CKM_WRITE_NEW_VALUE = 5;

    private EditText cfsrfidBatch;
    private EditText cfsrfidDateY;
    private EditText cfsrfidDateM;
    private EditText cfsrfidDateD;
    private EditText cfsrfidSupplier;
    private Spinner cfsrfidMaterial;
    private EditText cfsrfidColor;
    private EditText cfsrfidNumber;
    private EditText cfsrfidReserve;
    private Button cfsrfidButtonColor;

    /**
     * Nodes (stats) MCT passes through during its startup.
     */
    private enum StartUpNode {
        FirstUseDialog, DonateDialog, HasNfc, HasMifareClassicSupport,
        HasNfcEnabled, HasExternalNfc, ExternalNfcServiceRunning,
        HandleNewIntent
    }

    /**
     * Check for NFC hardware and MIFARE Classic support.
     * The directory structure and the std. keys files will be created as well.
     * Also, at the first run of this App, a warning
     * notice and a donate message will be displayed.
     * @see #initFolders()
     * @see #copyStdKeysFiles()
     */
    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // Show App version and footer.
        TextView tv = findViewById(R.id.textViewMainFooter);
        tv.setText(getString(R.string.app_version)
                + ": " + Common.getVersionCode());

        // Add the context menu to the tools button.
        Button tools = findViewById(R.id.buttonMainTools);
        registerForContextMenu(tools);

        cfsrfidBatch = findViewById(R.id.cfsrfidBatch);
        cfsrfidDateY = findViewById(R.id.cfsrfidDateY);
        cfsrfidDateM = findViewById(R.id.cfsrfidDateM);
        cfsrfidDateD = findViewById(R.id.cfsrfidDateD);
        cfsrfidSupplier = findViewById(R.id.cfsrfidSupplier);
        cfsrfidMaterial = findViewById(R.id.cfsrfidMaterial);
        cfsrfidColor = findViewById(R.id.cfsrfidColor);
        cfsrfidNumber = findViewById(R.id.cfsrfidNumber);
        cfsrfidReserve = findViewById(R.id.cfsrfidReserve);
        cfsrfidButtonColor = findViewById(R.id.cfsrfidButtonColor);

        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this,
            R.array.materials_array,
            android.R.layout.simple_spinner_item
        );
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        cfsrfidMaterial.setAdapter(adapter);
        cfsrfidMaterial.setSelection(29);

        cfsrfidColor.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {}

            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                onColorUpdate();
            }
        });


        // Restore state.
//        if (savedInstanceState != null) {
//            mDonateDialogWasShown = savedInstanceState.getBoolean(
//                    "donate_dialog_was_shown");
//            mInfoExternalNfcDialogWasShown = savedInstanceState.getBoolean(
//                    "info_external_nfc_dialog_was_shown");
//            mHasNoNfc = savedInstanceState.getBoolean("has_no_nfc");
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                mOldIntent = savedInstanceState.getParcelable("old_intent", Intent.class );
//            } else {
//                mOldIntent = savedInstanceState.getParcelable("old_intent");
//            }
//
//        }

        // Bind main layout buttons.
        mReadTag = findViewById(R.id.buttonMainReadTag);
        mWriteTag = findViewById(R.id.buttonMainWriteTag);
        mKeyEditor = findViewById(R.id.buttonMainEditKeyDump);
        mDumpEditor = findViewById(R.id.buttonMainEditCardDump);

        initFolders();
        copyStdKeysFiles();
    }



    /**
     * Save important state data before this activity gets destroyed.
     * @param outState The state to put data into.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("donate_dialog_was_shown", mDonateDialogWasShown);
        outState.putBoolean("info_external_nfc_dialog_was_shown", mInfoExternalNfcDialogWasShown);
        outState.putBoolean("has_no_nfc", mHasNoNfc);
        outState.putParcelable("old_intent", mOldIntent);
    }

    /**
     * Each phase of the MCTs startup is called "node" (see {@link StartUpNode})
     * and can be started by this function. The following nodes will be
     * started automatically (e.g. if the "has NFC support?" node is triggered
     * the "has MIFARE classic support?" node will be run automatically
     * after that).
     * @param startUpNode The node of the startup checks chain.
     * @see StartUpNode
     */
    private void runStartUpNode(StartUpNode startUpNode) {
        SharedPreferences sharedPref =
                getPreferences(Context.MODE_PRIVATE);
        Editor sharedEditor = sharedPref.edit();
        switch (startUpNode) {
            case FirstUseDialog:
                boolean isFirstRun = sharedPref.getBoolean(
                        "is_first_run", true);
                if (isFirstRun) {
                    createFirstUseDialog().show();
                } else {
                    runStartUpNode(StartUpNode.HasNfc);
                }
                break;
            case HasNfc:
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
                if (Common.getNfcAdapter() == null) {
                    mHasNoNfc = true;
                    runStartUpNode(StartUpNode.HasExternalNfc);
                } else {
                    runStartUpNode(StartUpNode.HasMifareClassicSupport);
                }
                break;
            case HasMifareClassicSupport:
                if (!Common.hasMifareClassicSupport()
                        && !Common.useAsEditorOnly()) {
                    runStartUpNode(StartUpNode.HasExternalNfc);
                } else {
                    runStartUpNode(StartUpNode.HasNfcEnabled);
                }
                break;
            case HasNfcEnabled:
                Common.setNfcAdapter(NfcAdapter.getDefaultAdapter(this));
                if (!Common.getNfcAdapter().isEnabled()) {
                    if (!Common.useAsEditorOnly()) {
                        createNfcEnableDialog().show();
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog);
                    }
                } else {
                    // Use MCT with internal NFC controller.
                    useAsEditorOnly(false);
                    Common.enableNfcForegroundDispatch(this);
                    runStartUpNode(StartUpNode.DonateDialog);
                }
                break;
            case HasExternalNfc:
                if (!Common.hasExternalNfcInstalled(this)
                        && !Common.useAsEditorOnly()) {
                    if (mHasNoNfc) {
                        // Here because the phone is not NFC enabled.
                        createInstallExternalNfcDialog().show();
                    } else {
                        // Here because phone does not support MIFARE Classic.
                        AlertDialog ad = createHasNoMifareClassicSupportDialog();
                        ad.show();
                        // Make links clickable.
                        ((TextView) ad.findViewById(android.R.id.message))
                                .setMovementMethod(
                                        LinkMovementMethod.getInstance());
                    }
                } else {
                    runStartUpNode(StartUpNode.ExternalNfcServiceRunning);
                }
                break;
            case ExternalNfcServiceRunning:
                int isExternalNfcRunning =
                        Common.isExternalNfcServiceRunning(this);
                if (isExternalNfcRunning == 0) {
                    // External NFC is not running.
                    if (!Common.useAsEditorOnly()) {
                        createStartExternalNfcServiceDialog().show();
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog);
                    }
                } else if (isExternalNfcRunning == 1) {
                    // External NFC is running. Use MCT with External NFC.
                    useAsEditorOnly(false);
                    runStartUpNode(StartUpNode.DonateDialog);
                } else {
                    // Can not check if External NFC is running.
                    if (!Common.useAsEditorOnly()
                            && !mInfoExternalNfcDialogWasShown) {
                        createInfoExternalNfcServiceDialog().show();
                        mInfoExternalNfcDialogWasShown = true;
                    } else {
                        runStartUpNode(StartUpNode.DonateDialog);
                    }
                }
                break;
            case DonateDialog:
                if (Common.IS_DONATE_VERSION) {
                    runStartUpNode(StartUpNode.HandleNewIntent);
                    break;
                }
                if (mDonateDialogWasShown) {
                    runStartUpNode(StartUpNode.HandleNewIntent);
                    break;
                }
                int currentVersion = 0;
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        currentVersion = (int) PackageInfoCompat.getLongVersionCode(
                            getPackageManager().getPackageInfo(getPackageName(),
                                PackageManager.PackageInfoFlags.of(0)));
                    } else {
                        currentVersion = (int) PackageInfoCompat.getLongVersionCode(
                            getPackageManager().getPackageInfo(getPackageName(), 0));
                    }
                } catch (NameNotFoundException e) {
                    Log.d(LOG_TAG, "Version not found.");
                }
                int lastVersion = sharedPref.getInt("mct_version",
                        currentVersion - 1);
                boolean showDonateDialog = sharedPref.getBoolean(
                        "show_donate_dialog", true);

                if (lastVersion < currentVersion || showDonateDialog) {
                    // This is either a new version of MCT or the user
                    // wants to see the donate dialog.
                    if (lastVersion < currentVersion) {
                        // Update the version.
                        sharedEditor.putInt("mct_version", currentVersion);
                        sharedEditor.putBoolean("show_donate_dialog", true);
                        sharedEditor.apply();
                    }
                    createDonateDialog().show();
                    mDonateDialogWasShown = true;
                } else {
                    runStartUpNode(StartUpNode.HandleNewIntent);
                }
                break;
            case HandleNewIntent:
                Common.setPendingComponentName(null);
                Intent intent = getIntent();
                if (intent != null) {
                    boolean isIntentWithTag = intent.getAction().equals(
                            NfcAdapter.ACTION_TECH_DISCOVERED);
                    if (isIntentWithTag && intent != mOldIntent) {
                        // If MCT was called by another app or the dispatch
                        // system with a tag delivered by intent, handle it as
                        // new tag intent.
                        mOldIntent = intent;
                        onNewIntent(getIntent());
                    } else {
                        // Last node. Do nothing.
                        break;
                    }
                }
                break;
        }
    }

    /**
     * Set whether to use the app in editor only mode or not.
     * @param useAsEditorOnly True if the app should be used in editor
     * only mode.
     */
    private void useAsEditorOnly(boolean useAsEditorOnly) {
        Common.setUseAsEditorOnly(useAsEditorOnly);
        mReadTag.setEnabled(!useAsEditorOnly);
        mWriteTag.setEnabled(!useAsEditorOnly);
    }

    /**
     * Set the theme according to preferences or system settings.
     * Default to "dark" theme before Android 10.
     * Defualt to "follow system" theme for Android 10+.
     */
    private void setTheme() {
        SharedPreferences pref = Common.getPreferences();
        int themeID;
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Default to dark before Android 10.
            themeID = pref.getInt(Preferences.Preference.CustomAppTheme.toString(), 0);
        } else {
            // Follow system theme for Android 10+.
            themeID = pref.getInt(Preferences.Preference.CustomAppTheme.toString(), 2);
        }
        switch (themeID) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
        }
    }

    /**
     * Create the dialog which is displayed once the app was started for the
     * first time. After showing the dialog, {@link #runStartUpNode(StartUpNode)}
     * with {@link StartUpNode#HasNfc} will be called.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createFirstUseDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_first_run_title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setMessage(R.string.dialog_first_run)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> dialog.cancel())
                .setOnCancelListener(
                        dialog -> {
                            SharedPreferences sharedPref =
                                    getPreferences(Context.MODE_PRIVATE);
                            Editor sharedEditor = sharedPref.edit();
                            sharedEditor.putBoolean("is_first_run", false);
                            sharedEditor.apply();
                            // Continue with "has NFC" check.
                            runStartUpNode(StartUpNode.HasNfc);
                        })
                .create();
    }

    /**
     * Create the dialog which is displayed if the device does not have
     * MIFARE classic support. After showing the dialog,
     * {@link #runStartUpNode(StartUpNode)} with {@link StartUpNode#DonateDialog}
     * will be called or the app will be exited.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createHasNoMifareClassicSupportDialog() {
        CharSequence styledText = HtmlCompat.fromHtml(
                getString(R.string.dialog_no_mfc_support_device),
                HtmlCompat.FROM_HTML_MODE_LEGACY);
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_mfc_support_device_title)
                .setMessage(styledText)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_install_external_nfc,
                (dialog, which) -> {
                    // Open Google Play for the donate version of MCT.
                    Uri uri = Uri.parse(
                            "market://details?id=eu.dedb.nfc.service");
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store"
                                        + "/apps/details?id=eu.dedb.nfc"
                                        + ".service")));
                    }
                })
                .setNeutralButton(R.string.action_editor_only,
                        (dialog, which) -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setNegativeButton(R.string.action_exit_app,
                        (dialog, id) -> {
                            // Exit the App.
                            finish();
                        })
                .setOnCancelListener(
                        dialog -> finish())
                .create();
    }

    /**
     * Create a dialog that send user to NFC settings if NFC is off.
     * Alternatively the user can chose to use the App in editor only
     * mode or exit the App.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createNfcEnableDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_nfc_not_enabled_title)
                .setMessage(R.string.dialog_nfc_not_enabled)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton(R.string.action_nfc,
                        (dialog, which) -> {
                            // Goto NFC Settings.
                            startActivity(new Intent(
                                    Settings.ACTION_NFC_SETTINGS));
                        })
                .setNeutralButton(R.string.action_editor_only,
                        (dialog, which) -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setNegativeButton(R.string.action_exit_app,
                        (dialog, id) -> {
                            // Exit the App.
                            finish();
                        })
                .setOnCancelListener(
                        dialog -> finish())
                .create();
    }

    /**
     * Create the dialog which is displayed if the device has not "External NFC"
     * installed. After showing the dialog, {@link #runStartUpNode(StartUpNode)}
     * with {@link StartUpNode#DonateDialog} will be called or MCT will
     * redirect the user to the play store page of "External NFC"  or
     * the app will be exited.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createInstallExternalNfcDialog() {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_no_nfc_support_title)
                .setMessage(R.string.dialog_no_nfc_support)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_install_external_nfc,
                        (dialog, which) -> {
                            // Open Google Play for the donate version of MCT.
                            Uri uri = Uri.parse(
                                    "market://details?id=eu.dedb.nfc.service");
                            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                            try {
                                startActivity(goToMarket);
                            } catch (ActivityNotFoundException e) {
                                startActivity(new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("https://play.google.com/store"
                                                + "/apps/details?id=eu.dedb.nfc"
                                                + ".service")));
                            }
                        })
                .setNeutralButton(R.string.action_editor_only,
                        (dialog, which) -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setNegativeButton(R.string.action_exit_app,
                        (dialog, id) -> {
                            // Exit the App.
                            finish();
                        })
                .setOnCancelListener(
                        dialog -> finish())
                .create();
    }

    /**
     * Create the dialog which is displayed if the "External NFC" service is
     * not running. After showing the dialog,
     * {@link #runStartUpNode(StartUpNode)} with {@link StartUpNode#DonateDialog}
     * will be called or MCT will redirect the user to the settings of
     * "External NFC" or the app will be exited.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createStartExternalNfcServiceDialog() {
        final Context context = this;
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_start_external_nfc_title)
                .setMessage(R.string.dialog_start_external_nfc)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_start_external_nfc,
                        (dialog, which) -> {
                            useAsEditorOnly(true);
                            Common.openApp(context, "eu.dedb.nfc.service");
                        })
                .setNeutralButton(R.string.action_editor_only,
                        (dialog, which) -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setNegativeButton(R.string.action_exit_app,
                        (dialog, id) -> {
                            // Exit the App.
                            finish();
                        })
                .setOnCancelListener(
                        dialog -> finish())
                .create();
    }

    /**
     * Create the dialog which is displayed if it is not clear if the
     * "External NFC" service running. After showing the dialog,
     * {@link #runStartUpNode(StartUpNode)} with {@link StartUpNode#DonateDialog}
     * will be called or MCT will redirect the user to the settings of
     * "External NFC".
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createInfoExternalNfcServiceDialog() {
        final Context context = this;
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_info_external_nfc_title)
                .setMessage(R.string.dialog_info_external_nfc)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_external_nfc_is_running,
                        (dialog, which) -> {
                            // External NFC is running. Do "nothing".
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setNeutralButton(R.string.action_start_external_nfc,
                        (dialog, which) -> Common.openApp(context, "eu.dedb.nfc.service"))
                .setNegativeButton(R.string.action_editor_only,
                        (dialog, id) -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .setOnCancelListener(
                        dialog -> {
                            // Only use Editor.
                            useAsEditorOnly(true);
                            runStartUpNode(StartUpNode.DonateDialog);
                        })
                .create();
    }

    /**
     * Create the donate dialog. After showing the dialog,
     * {@link #runStartUpNode(StartUpNode)} with
     * {@link StartUpNode#HandleNewIntent} will be called.
     * @return The created alert dialog.
     * @see #runStartUpNode(StartUpNode)
     */
    private AlertDialog createDonateDialog() {
        View dialogLayout = getLayoutInflater().inflate(
                R.layout.dialog_donate,
                findViewById(android.R.id.content), false);
        TextView textView = dialogLayout.findViewById(
                R.id.textViewDonateDialog);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        final CheckBox showDonateDialogCheckBox = dialogLayout
                .findViewById(R.id.checkBoxDonateDialog);
        return new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_donate_title)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setView(dialogLayout)
                .setPositiveButton(R.string.action_ok,
                        (dialog, which) -> dialog.cancel())
                .setOnCancelListener(
                        dialog -> {
                            if (showDonateDialogCheckBox.isChecked()) {
                                // Do not show the donate dialog again.
                                SharedPreferences sharedPref =
                                        getPreferences(Context.MODE_PRIVATE);
                                Editor sharedEditor = sharedPref.edit();
                                sharedEditor.putBoolean(
                                        "show_donate_dialog", false);
                                sharedEditor.apply();
                            }
                            runStartUpNode(StartUpNode.HandleNewIntent);
                        })
                .create();
    }

    /**
     * Create the directories needed by MCT and clean out the tmp folder.
     */
    @SuppressLint("ApplySharedPref")
    private void initFolders() {
        // Create keys directory.
        File path = Common.getFile(Common.KEYS_DIR);

        if (!path.exists() && !path.mkdirs()) {
            // Could not create directory.
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                    + "/" + Common.KEYS_DIR + "' directory.");
            return;
        }

        // Create dumps directory.
        path = Common.getFile(Common.DUMPS_DIR);
        if (!path.exists() && !path.mkdirs()) {
            // Could not create directory.
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                    + "/" + Common.DUMPS_DIR + "' directory.");
            return;
        }

        // Create tmp directory.
        path = Common.getFile(Common.TMP_DIR);
        if (!path.exists() && !path.mkdirs()) {
            // Could not create directory.
            Log.e(LOG_TAG, "Error while creating '" + Common.HOME_DIR
                    + Common.TMP_DIR + "' directory.");
            return;
        }
        // Try to clean up tmp directory.
        File[] tmpFiles = path.listFiles();
        if (tmpFiles != null) {
            for (File file : tmpFiles) {
                file.delete();
            }
        }
    }

    /**
     * Add a menu with "preferences", "about", etc. to the Activity.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu_functions, menu);
        return true;
    }

    /**
     * Add the menu with the tools.
     * It will be shown if the user clicks on "Tools".
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        menu.setHeaderTitle(R.string.dialog_tools_menu_title);
        menu.setHeaderIcon(android.R.drawable.ic_menu_preferences);
        inflater.inflate(R.menu.tools, menu);
        // Enable/Disable tag info tool depending on NFC availability.
        menu.findItem(R.id.menuMainTagInfo).setEnabled(
                !Common.useAsEditorOnly());
        // Enable/Disable UID clone info tool depending on NFC availability.
        menu.findItem(R.id.menuMainCloneUidTool).setEnabled(
                !Common.useAsEditorOnly());
    }

    /**
     * Resume by triggering MCT's startup system
     * ({@link #runStartUpNode(StartUpNode)}).
     * @see #runStartUpNode(StartUpNode)
     */
    @Override
    public void onResume() {
        super.onResume();

        mKeyEditor.setEnabled(true);
        mDumpEditor.setEnabled(true);
        useAsEditorOnly(Common.useAsEditorOnly());
        // The start up nodes will also enable the NFC foreground dispatch if all
        // conditions are met (has NFC & NFC enabled).
        runStartUpNode(StartUpNode.FirstUseDialog);
    }

    /**
     * Disable NFC foreground dispatch system.
     * @see Common#disableNfcForegroundDispatch(Activity)
     */
    @Override
    public void onPause() {
        super.onPause();
        Common.disableNfcForegroundDispatch(this);
    }

    /**
     * Handle new Intent as a new tag Intent and if the tag/device does not
     * support MIFARE Classic, then run {@link TagInfoTool}.
     * @see Common#treatAsNewTag(Intent, android.content.Context)
     * @see TagInfoTool
     */
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Common.getPendingComponentName() != null) {
            intent.setComponent(Common.getPendingComponentName());
            startActivity(intent);
        } else {
            int typeCheck = Common.treatAsNewTag(intent, this);
            if (typeCheck == -1 || typeCheck == -2) {
                // Device or tag does not support MIFARE Classic.
                // Run the only thing that is possible: The tag info tool.
                Intent i = new Intent(this, TagInfoTool.class);
                startActivity(i);
            }
        }
    }

    /**
     * Show the {@link ReadTag}.
     * @param view The View object that triggered the method
     * (in this case the read tag button).
     * @see ReadTag
     */
    public void onShowReadTag(View view) {
        Intent intent = new Intent(this, ReadTag.class);
        startActivity(intent);
    }

    /**
     * Show the {@link WriteTag}.
     * @param view The View object that triggered the method
     * (in this case the write tag button).
     * @see WriteTag
     */
    public void onShowWriteTag(View view) {
        Intent intent = new Intent(this, WriteTag.class);
        startActivity(intent);
    }

    /**
     * Show the {@link HelpAndInfo}.
     * @param view The View object that triggered the method
     * (in this case the help/info button).
     */
    public void onShowHelp(View view) {
        Intent intent = new Intent(this, HelpAndInfo.class);
        startActivity(intent);
    }

    /**
     * Show the tools menu (as context menu).
     * @param view The View object that triggered the method
     * (in this case the tools button).
     */
    public void onShowTools(View view) {
        openContextMenu(view);
    }

    /**
     * Open a file chooser ({@link FileChooser}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * If the dump files folder is empty display an additional error
     * message.
     * @param view The View object that triggered the method
     * (in this case the show/edit tag dump button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenTagDumpEditor(View view) {
        File file = Common.getFile(Common.DUMPS_DIR);
        if (file.isDirectory() && (file.listFiles() == null
                || file.listFiles().length == 0)) {
            Toast.makeText(this, R.string.info_no_dumps,
                Toast.LENGTH_LONG).show();
        }
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR, file.getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_dump_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_dump_file));
        startActivityForResult(intent, FILE_CHOOSER_DUMP_FILE);
    }

    /**
     * Open a file chooser ({@link FileChooser}). The
     * Activity result will be processed in
     * {@link #onActivityResult(int, int, Intent)}.
     * @param view The View object that triggered the method
     * (in this case the show/edit key button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onOpenKeyEditor(View view) {
        Intent intent = new Intent(this, FileChooser.class);
        intent.putExtra(FileChooser.EXTRA_DIR,
                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(FileChooser.EXTRA_TITLE,
                getString(R.string.text_open_key_file_title));
        intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
                getString(R.string.action_open_key_file));
        intent.putExtra(FileChooser.EXTRA_ALLOW_NEW_FILE, true);
        startActivityForResult(intent, FILE_CHOOSER_KEY_FILE);
    }

    /**
     * Show the {@link Preferences}.
     */
    private void onShowPreferences() {
        Intent intent = new Intent(this, Preferences.class);
        startActivity(intent);
    }

    /**
     * Show the about dialog.
     */
    private void onShowAboutDialog() {
        CharSequence styledText = HtmlCompat.fromHtml(
                getString(R.string.dialog_about_mct, Common.getVersionCode()),
                HtmlCompat.FROM_HTML_MODE_LEGACY);
        AlertDialog ad = new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_about_mct_title)
            .setMessage(styledText)
            .setIcon(R.mipmap.ic_launcher)
            .setPositiveButton(R.string.action_ok,
                    (dialog, which) -> {
                        // Do nothing.
                    }).create();
         ad.show();
         // Make links clickable.
         ((TextView)ad.findViewById(android.R.id.message)).setMovementMethod(
                 LinkMovementMethod.getInstance());
    }

    /**
     * Handle the user input from the general options menu
     * (e.g. show the about dialog).
     * @see #onShowAboutDialog()
     * @see #onShowPreferences()
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        int id = item.getItemId();
        if (id == R.id.menuMainPreferences) {
            onShowPreferences();
            return true;
        } else if (id == R.id.menuMainAbout) {
            onShowAboutDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Handle (start) the selected tool from the tools menu.
     * @see TagInfoTool
     * @see ValueBlockTool
     * @see AccessConditionTool
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Intent intent;
        int id = item.getItemId();
        if (id == R.id.menuMainTagInfo) {
            intent = new Intent(this, TagInfoTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainValueBlockTool) {
            intent = new Intent(this, ValueBlockTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainAccessConditionTool) {
            intent = new Intent(this, AccessConditionTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainDiffTool) {
            intent = new Intent(this, DiffTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainBccTool) {
            intent = new Intent(this, BccTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainCloneUidTool) {
            intent = new Intent(this, CloneUidTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainImportExportTool) {
            intent = new Intent(this, ImportExportTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainUidLogTool) {
            intent = new Intent(this, UidLogTool.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menuMainDataConversionTool) {
            intent = new Intent(this, DataConversionTool.class);
            startActivity(intent);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * Run the {@link DumpEditor} or the {@link KeyEditor}
     * if file chooser result is O.K.
     * @see DumpEditor
     * @see KeyEditor
     * @see #onOpenTagDumpEditor(View)
     * @see #onOpenKeyEditor(View)
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case CKM_WRITE_BLOCK:
                if (resultCode != Activity.RESULT_OK) {
                    // Error.
                    //ckmError = resultCode;
                } else {
                    // Write block.
                    writeBlock();
                }
                break;
        }

//        Intent openMainActivity = new Intent(this, MainMenu.class);
//        openMainActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        startActivityIfNeeded(openMainActivity, 0);

//        case FILE_CHOOSER_DUMP_FILE:
//            if (resultCode == Activity.RESULT_OK) {
//                Intent intent = new Intent(this, DumpEditor.class);
//                intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE,
//                        data.getStringExtra(
//                                FileChooser.EXTRA_CHOSEN_FILE));
//                startActivity(intent);
//            }
//            break;
//        case FILE_CHOOSER_KEY_FILE:
//            if (resultCode == Activity.RESULT_OK) {
//                Intent intent = new Intent(this, KeyEditor.class);
//                intent.putExtra(FileChooser.EXTRA_CHOSEN_FILE,
//                        data.getStringExtra(
//                                FileChooser.EXTRA_CHOSEN_FILE));
//                startActivity(intent);
//            }
//            break;
//        }
    }

    private String toHex(String arg) {
        return String.format("%040x", new BigInteger(1, arg.getBytes(/*YOUR_CHARSET?*/)));
    }

    private String getDataString(){

        String material = cfsrfidMaterial.getSelectedItem().toString().substring(0, 5);
        String data = cfsrfidBatch.getText().toString() + cfsrfidDateY.getText().toString() + cfsrfidDateM.getText().toString() + cfsrfidDateD.getText().toString() + cfsrfidSupplier.getText().toString() + material + cfsrfidColor.getText().toString() + cfsrfidNumber.getText().toString() + cfsrfidReserve.getText().toString();
        //Pad the string so there's enough trailing zeros
        data = data + "00000000000000000000000000000000";
        Log.d("CFSRFID", "getDataString.rawdata: " + data);
        data = toHex(data);
        Log.d("CFSRFID", "getDataString.hexdata: " + data);
        return data;
    }

    private boolean goodData(){

        String material = cfsrfidMaterial.getSelectedItem().toString().substring(0, 5);
        Log.d("CFSRFID", "Material: " + material);

        if (cfsrfidBatch.getText().toString().length()!=3){
            return false;
        }
        if (cfsrfidDateY.getText().toString().length()!=2){
            return false;
        }
        if (cfsrfidDateM.getText().toString().length()!=1){
            return false;
        }
        if (cfsrfidDateD.getText().toString().length()!=2){
            return false;
        }
        if (cfsrfidSupplier.getText().toString().length()!=4){
            return false;
        }
        if (material.length()!=5){
            return false;
        }
        if (cfsrfidColor.getText().toString().length()!=7){
            return false;
        }
        if (cfsrfidNumber.getText().toString() .length()!=6){
            return false;
        }
        //No need to check Reserve, we pad the end with 0s
        //if (cfsrfidReserve.getText().toString().length()!=3){return false;}

        return true;
    }

    public void onSetDefaults(View view)
    {
        cfsrfidMaterial.setSelection(29);
        cfsrfidBatch.setText("7A3");
        cfsrfidDateY.setText("24");
        cfsrfidDateM.setText("1");
        cfsrfidDateD.setText("20");
        cfsrfidSupplier.setText("0A21");
        cfsrfidColor.setText("#0000FF");
        cfsrfidNumber.setText("016500");
        cfsrfidReserve.setText("0001000000000000000");
    }

    private boolean checkColor(String str){
        if (str.charAt(0) != '#')
            return false;

        if (!(str.length() == 7))
            return false;

        for (int i = 1; i < str.length(); i++)
            if (!((str.charAt(i) >= '0' && str.charAt(i) <= 9)
                || (str.charAt(i) >= 'a' && str.charAt(i) <= 'f')
                || (str.charAt(i) >= 'A' || str.charAt(i) <= 'F')))
                return false;
        return true;
    }
    public void onColorPicker(View view){

        //TODO: .setInitialColor(color);
        new ColorPickerDialog.Builder(this)
            .setTitle("ColorPicker Dialog")
            .setPreferenceName("MyColorPickerDialog")
            .setPositiveButton(getString(R.string.action_ok),
                new ColorEnvelopeListener() {
                    @Override
                    public void onColorSelected(ColorEnvelope envelope, boolean fromUser) {
                        //setLayoutColor(envelope);
                        cfsrfidColor.setText("#" + envelope.getHexCode().substring(2,8));
                    }
                })
            .setNegativeButton(getString(R.string.action_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                })
            .attachAlphaSlideBar(true) // the default value is true.
            .attachBrightnessSlideBar(true)  // the default value is true.
            .setBottomSpace(12) // set a bottom space between the last slidebar and buttons.
            .show();
    }

    public void onColorUpdate(){
        if (checkColor(cfsrfidColor.getText().toString())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int red = Integer.decode("0x" +cfsrfidColor.getText().toString().substring(1,3));
                int blue = Integer.decode("0x" +cfsrfidColor.getText().toString().substring(3,5));
                int green = Integer.decode("0x" +cfsrfidColor.getText().toString().substring(5,7));
                int color = Color.rgb(red, green, blue);
                ColorDrawable cd = new ColorDrawable(color);
                cfsrfidButtonColor.setForeground(cd);
            }
        }
    }



    /**
     * Check the user input and, if necessary, possible issues with block 0.
     * If everything is O.K., show the {@link KeyMapCreator} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created, {@link #writeBlock()} will be triggered.
     * @param view The View object that triggered the method
     * (in this case the write block button).
     * @see KeyMapCreator
     * @see #checkBlock0(String, boolean)
     * @see #checkAccessConditions(String, boolean)
     * @see #createKeyMapForBlock(int, boolean)
     */
    public void onWriteBlock(View view) {
        // Check input.
        //if (!checkSectorAndBlock(mSectorTextBlock, mBlockTextBlock)) {
        //    return;
        //}
        String data = getDataString();
        String data1 = data.substring(0,32);
        String data2 = data.substring(32,64);
        String data3 = data.substring(64,96);
        Log.d("CFSRFID", "onwriteBlock.data1: " + data1);
        Log.d("CFSRFID", "onwriteBlock.data1: " + data2);
        Log.d("CFSRFID", "onwriteBlock.data1: " + data3);

        if (!goodData()){
            Toast.makeText(this, "Invalid data entered, try again with defaults",
                Toast.LENGTH_LONG).show();
            return;
        }
        if (!Common.isHexAnd16Byte(data1, this)) {
            return;
        }
        if (!Common.isHexAnd16Byte(data2, this)) {
            return;
        }
        if (!Common.isHexAnd16Byte(data3, this)) {
            return;
        }

        final int sector = 1;
        final int block = 0; //0, 1, 2

        if (!isSectorInRage(this, true)) {
            return;
        }

        if (block == 3 || block == 15) {
            // Sector Trailer.
            int acCheck = checkAccessConditions(data, true);
            if (acCheck == 1) {
                // Invalid Access Conditions. Abort.
                return;
            }
            // Warning. This is a sector trailer.
            new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_sector_trailer_warning_title)
                .setMessage(R.string.dialog_sector_trailer_warning)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.action_i_know_what_i_am_doing,
                    (dialog, which) -> {
                        // Show key map creator.
                        createKeyMapForBlock(sector, false);
                    })
                .setNegativeButton(R.string.action_cancel,
                    (dialog, id) -> {
                        // Do nothing.
                    }).show();
        } else if (sector == 0 && block == 0) {
            // Manufacturer block.
            // Is block 0 valid? Display warning.
            int block0Check = checkBlock0(data, true);
            if (block0Check == 1 || block0Check == 2) {
                // BCC not valid. Abort.
                return;
            }
            // Warning. Writing to manufacturer block.
            showWriteManufInfo(true);
        } else {
            // Normal data block.
            createKeyMapForBlock(sector, false);
        }
    }


    /**
     * Check the user input of the sector and the block field. This is a
     * helper function for {@link #onWriteBlock(View)} and
     * {@link #onWriteValue(View)}.
     * @param sector Sector input field.
     * @param block Block input field.
     * @return True if both values are okay. False otherwise.
     */
    private boolean checkSectorAndBlock(EditText sector, EditText block) {
        if (sector.getText().toString().isEmpty()
            || block.getText().toString().isEmpty()) {
            // Error, location not fully set.
            Toast.makeText(this, R.string.info_data_location_not_set,
                Toast.LENGTH_LONG).show();
            return false;
        }
        int sectorNr = Integer.parseInt(sector.getText().toString());
        int blockNr = Integer.parseInt(block.getText().toString());
        if (sectorNr > KeyMapCreator.MAX_SECTOR_COUNT-1
            || sectorNr < 0) {
            // Error, sector is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_sector_out_of_range,
                Toast.LENGTH_LONG).show();
            return false;
        }
        if (blockNr > KeyMapCreator.MAX_BLOCK_COUNT_PER_SECTOR-1
            || blockNr < 0) {
            // Error, block is out of range for any MIFARE tag.
            Toast.makeText(this, R.string.info_block_out_of_range,
                Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


    /**
     * Show or hide the options section of write dump.
     * @param view The View object that triggered the method
     * (in this case the show options button).
     */
    public void onShowOptions(View view) {
        LinearLayout ll = findViewById(R.id.linearLayoutWriteTagDumpOptions);
        CheckBox cb = findViewById(R.id.checkBoxWriteTagDumpOptions);
        if (cb.isChecked()) {
            ll.setVisibility(View.VISIBLE);
        } else {
            ll.setVisibility(View.GONE);
        }
    }

    /**
     * Display information about writing to the manufacturer block.
     * @param view The View object that triggered the method
     * (in this case the info on write-to-manufacturer button).
     * @see #showWriteManufInfo(boolean)
     */
    public void onShowWriteManufInfo(View view) {
        showWriteManufInfo(false);
    }

    /**
     * Display information about writing to the manufacturer block and
     * optionally create a key map for the first sector.
     * @param createKeyMap If true {@link #createKeyMapForBlock(int, boolean)}
     * will be triggered the time the user confirms the dialog.
     */
    private void showWriteManufInfo(final boolean createKeyMap) {
        // Warning. Writing to the manufacturer block is not normal.
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.dialog_block0_writing_title);
        dialog.setMessage(R.string.dialog_block0_writing);
        dialog.setIcon(android.R.drawable.ic_dialog_info);

        int buttonID = R.string.action_ok;
        if (createKeyMap) {
            buttonID = R.string.action_i_know_what_i_am_doing;
            dialog.setNegativeButton(R.string.action_cancel,
                (dialog12, which) -> {
                    // Do nothing.
                });
        }
        dialog.setPositiveButton(buttonID,
            (dialog1, which) -> {
                // Do nothing or create a key map.
                if (createKeyMap) {
                    createKeyMapForBlock(0, false);
                }
            });
        dialog.show();
    }

    /**
     * Check if block 0 data is valid and show a error message if needed.
     * @param block0 Hex string of block 0.
     * @param showToasts If true, show error mesages as toast.
     * @return <ul>
     * <li>0 - Everything is O.K.</li>
     * <li>1 - There is no tag.</li>
     * <li>2 - BCC is not valid.</li>
     * <li>3 - SAK or ATQA is not valid.</li>
     * </ul>
     */
    private int checkBlock0(String block0, boolean showToasts) {
        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            // Error. There is no tag.
            return 1;
        }
        reader.close();
        int uidLen = Common.getUID().length;

        // BCC.
        if (uidLen == 4 ) {
            byte bcc = Common.hex2Bytes(block0.substring(8, 10))[0];
            byte[] uid = Common.hex2Bytes(block0.substring(0, 8));
            boolean isValidBcc = Common.isValidBcc(uid, bcc);
            if (!isValidBcc) {
                // Error. BCC is not valid. Show error message.
                if (showToasts) {
                    Toast.makeText(this, R.string.info_bcc_not_valid,
                        Toast.LENGTH_LONG).show();
                }
                return 2;
            }
        }

        // SAK & ATQA.
        boolean isValidBlock0 = Common.isValidBlock0(
            block0, uidLen, reader.getSize(), true);
        if (!isValidBlock0) {
            if (showToasts) {
                Toast.makeText(this, R.string.text_block0_warning,
                    Toast.LENGTH_LONG).show();
            }
            return 3;
        }

        // Everything was O.K.
        return 0;
    }

    /**
     * Check if the Access Conditions of a Sector Trailer are correct and
     * if they are irreversible (shows a error message if needed).
     * @param sectorTrailer The Sector Trailer as hex string.
     * @param showToasts If true, show error mesages as toast.
     * @return <ul>
     * <li>0 - Everything is O.K.</li>
     * <li>1 - The Access Conditions are invalid.</li>
     * <li>2 - The Access Conditions are irreversible.</li>
     * </ul>
     */
    private int checkAccessConditions(String sectorTrailer, boolean showToasts) {
        // Check if Access Conditions are valid.
        byte[] acBytes = Common.hex2Bytes(sectorTrailer.substring(12, 18));
        byte[][] acMatrix = Common.acBytesToACMatrix(acBytes);
        if (acMatrix == null) {
            // Error. Invalid ACs.
            if (showToasts) {
                Toast.makeText(this, R.string.info_ac_format_error,
                    Toast.LENGTH_LONG).show();
            }
            return 1;
        }
        // Check if Access Conditions are irreversible.
        boolean keyBReadable = Common.isKeyBReadable(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3]);
        int writeAC = Common.getOperationRequirements(
            acMatrix[0][3], acMatrix[1][3], acMatrix[2][3],
            Common.Operation.WriteAC, true, keyBReadable);
        if (writeAC == 0) {
            // Warning. Access Conditions can not be changed after writing.
            if (showToasts) {
                Toast.makeText(this, R.string.info_irreversible_acs,
                    Toast.LENGTH_LONG).show();
            }
            return 2;
        }
        return 0;
    }

    /**
     * Display information about using custom Access Conditions for all
     * sectors of the dump.
     * @param view The View object that triggered the method
     * (in this case the info on "use-static-access-conditions" button).
     */
    public void onShowStaticACInfo(View view) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_static_ac_title)
            .setMessage(R.string.dialog_static_ac)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(R.string.action_ok,
                (dialog, which) -> {
                    // Do nothing.
                }).show();
    }

    /**
     * Helper function for {@link #onWriteBlock(View)} and
     * {@link #onWriteValue(View)} to show
     * the {@link KeyMapCreator}.
     * @param sector The sector for the mapping range of
     * {@link KeyMapCreator}
     * @param isValueBlock If true, the key map will be created for a Value
     * Block ({@link #writeValueBlock()}).
     * @see KeyMapCreator
     * @see #onWriteBlock(View)
     * @see #onWriteValue(View)
     */
    private void createKeyMapForBlock(int sector, boolean isValueBlock) {
        //TODO: Always use std keys

        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
            Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, sector);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO, sector);
        if (isValueBlock) {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                R.string.action_create_key_map_and_write_value_block));
            startActivityForResult(intent, CKM_WRITE_NEW_VALUE);
        } else {
            intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT, getString(
                R.string.action_create_key_map_and_write_block));
            startActivityForResult(intent, CKM_WRITE_BLOCK);
        }
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to write the given
     * data to the tag. Possible errors are displayed to the user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteBlock(View)
     */
    private void writeBlock(){
        String data = getDataString();
        String data1 = data.substring(0,32);
        String data2 = data.substring(32,64);
        String data3 = data.substring(64,96);
        Log.d("CFSRFID", "writeBlock.data1: " + data1);
        Log.d("CFSRFID", "writeBlock.data2: " + data2);
        Log.d("CFSRFID", "writeBlock.data3: " + data3);
        //TODO: Actually write
        writeBlockCFSRFID(data1,0);
        writeBlockCFSRFID(data2,1);
        writeBlockCFSRFID(data3,2);
    }

    private void writeBlockCFSRFID(String data, int block) {



        MCReader reader = Common.checkForTagAndCreateReader(this);
        if (reader == null) {
            return;
        }
        int sector = 1;
        //int block = 0; //0, 1, 2
        //String data = data1;
        byte[][] keys = Common.getKeyMap().get(sector);
        int result = -1;
        int resultKeyA = -1;
        int resultKeyB = -1;

        // Write. There are some tags which report a successful write, although
        // the write was not successful. Therefore, we try to write it using both keys.
        // Try key B first.
        if (keys[1] != null) {
            resultKeyB = reader.writeBlock(sector, block,
                Common.hex2Bytes(data),
                keys[1], true);
        }
        // Try to write with key A (if there is one).
        if (keys[0] != null) {
            resultKeyA = reader.writeBlock(sector, block,
                Common.hex2Bytes(data),
                keys[0], false);
        }

        reader.close();
        if (resultKeyA == 0 || resultKeyB == 0) {
            result = 0;
        } else if (resultKeyA == 2 || resultKeyB == 2) {
            result = 2;
        } else if (resultKeyA == -1 || resultKeyB == -1) {
            result = -1;
        }

        // Error handling.
        switch (result) {
            case 2:
                Toast.makeText(this, R.string.info_block_not_in_sector,
                    Toast.LENGTH_LONG).show();
                return;
            case -1:
                Toast.makeText(this, R.string.info_error_writing_block,
                    Toast.LENGTH_LONG).show();
                return;
        }
        Toast.makeText(this, R.string.info_write_successful,
            Toast.LENGTH_LONG).show();
        //finish();
    }

    /**
     * Regular behavior: Check input, open a file chooser ({@link FileChooser})
     * to select a dump and wait for its result in
     * {@link #onActivityResult(int, int, Intent)}.
     * This method triggers the call chain: open {@link FileChooser}
     * (this method) -> read dump
     * -> check dump ({@link #checkDumpAndShowSectorChooserDialog(String[])}) ->
     * open {@link KeyMapCreator} ({@link #createKeyMapForDump()})
     * -> run {@link #checkDumpAgainstTag()} -> run
     * {@link #writeDump(HashMap, SparseArray)}.<br />
     * Behavior if the dump is already there (from the {@link DumpEditor}):
     * The same as before except the call chain will directly start from
     * {@link #checkDumpAndShowSectorChooserDialog(String[])}.<br />
     * (The static Access Conditions will be checked in any case, if the
     * option is enabled.)
     * @param view The View object that triggered the method
     * (in this case the write full dump button).
     * @see FileChooser
     * @see #onActivityResult(int, int, Intent)
     */
    public void onWriteDump(View view) {
//        // Check the static Access Condition option.
//        if (mEnableStaticAC.isChecked()) {
//            String ac = mStaticAC.getText().toString();
//            if (!ac.matches("[0-9A-Fa-f]+")) {
//                // Error, not hex.
//                Toast.makeText(this, R.string.info_ac_not_hex,
//                        Toast.LENGTH_LONG).show();
//                return;
//            }
//            if (ac.length() != 6) {
//                // Error, not 3 byte (6 chars).
//                Toast.makeText(this, R.string.info_ac_not_3_byte,
//                        Toast.LENGTH_LONG).show();
//                return;
//            }
//        }
//
//        if (mWriteDumpFromEditor) {
//            // Write dump directly from the dump editor.
//            // (Dump has already been chosen.)
//            checkDumpAndShowSectorChooserDialog(mDumpFromEditor);
//        } else {
//            // Show file chooser (chose dump).
//            Intent intent = new Intent(this, FileChooser.class);
//            intent.putExtra(FileChooser.EXTRA_DIR,
//                    Common.getFile(Common.DUMPS_DIR).getAbsolutePath());
//            intent.putExtra(FileChooser.EXTRA_TITLE,
//                    getString(R.string.text_open_dump_title));
//            intent.putExtra(FileChooser.EXTRA_CHOOSER_TEXT,
//                    getString(R.string.text_choose_dump_to_write));
//            intent.putExtra(FileChooser.EXTRA_BUTTON_TEXT,
//                    getString(R.string.action_write_full_dump));
//            startActivityForResult(intent, FC_WRITE_DUMP);
//        }
//    }
//
//    /**
//     * Read the dump and call {@link #checkDumpAndShowSectorChooserDialog(String[])}.
//     * @param pathToDump path and filename of the dump
//     * (selected by {@link FileChooser}).
//     * @see #checkDumpAndShowSectorChooserDialog(String[])
//     */
//    private void readDumpFromFile(String pathToDump) {
//        // Read dump.
//        File file = new File(pathToDump);
//        String[] dump = Common.readFileLineByLine(file, false, this);
//        checkDumpAndShowSectorChooserDialog(dump);
    }

    /**
     * Triggered after the dump was selected (by {@link FileChooser})
     * and read (by  this method saves
     * the data including its position in
     * If the "use static Access Condition" option is enabled, all the ACs
     * will be replaced by the static ones. After this it will show a dialog
     * in which the user can choose the sectors he wants
     * to write. When the sectors are chosen, this method calls
     * {@link #createKeyMapForDump()} to create a key map for the present tag.
     * @param dump Dump selected by {@link FileChooser} or directly
     * from the {@link DumpEditor} (via an Intent with{@link #EXTRA_DUMP})).
     * @see KeyMapCreator
     * @see #createKeyMapForDump()
     * @see #checkBlock0(String, boolean)
     */
    @SuppressLint("SetTextI18n")
    private void checkDumpAndShowSectorChooserDialog(final String[] dump) {
//        int err = Common.isValidDump(dump, false);
//        if (err != 0) {
//            // Error.
//            Common.isValidDumpErrorToast(err, this);
//            return;
//        }
//
//        initDumpWithPosFromDump(dump);
//
//        // Create and show sector chooser dialog
//        // (let the user select the sectors which will be written).
//        View dialogLayout = getLayoutInflater().inflate(
//                R.layout.dialog_write_sectors,
//                findViewById(android.R.id.content), false);
//        LinearLayout llCheckBoxes = dialogLayout.findViewById(
//                R.id.linearLayoutWriteSectorsCheckBoxes);
//        Button selectAll = dialogLayout.findViewById(
//                R.id.buttonWriteSectorsSelectAll);
//        Button selectNone = dialogLayout.findViewById(
//                R.id.buttonWriteSectorsSelectNone);
//        Integer[] sectors = mDumpWithPos.keySet().toArray(
//                new Integer[0]);
//        Arrays.sort(sectors);
//        final Context context = this;
//        final CheckBox[] sectorBoxes = new CheckBox[mDumpWithPos.size()];
//        for (int i = 0; i< sectors.length; i++) {
//            sectorBoxes[i] = new CheckBox(this);
//            sectorBoxes[i].setChecked(true);
//            sectorBoxes[i].setTag(sectors[i]);
//            sectorBoxes[i].setText(getString(R.string.text_sector)
//                    + " " + sectors[i]);
//            llCheckBoxes.addView(sectorBoxes[i]);
//        }
//        OnClickListener listener = v -> {
//            String tag = v.getTag().toString();
//            for (CheckBox box : sectorBoxes) {
//                box.setChecked(tag.equals("all"));
//            }
//        };
//        selectAll.setOnClickListener(listener);
//        selectNone.setOnClickListener(listener);
//
//        final AlertDialog dialog = new AlertDialog.Builder(this)
//            .setTitle(R.string.dialog_write_sectors_title)
//            .setIcon(android.R.drawable.ic_menu_edit)
//            .setView(dialogLayout)
//            .setPositiveButton(R.string.action_ok,
//                    (dialog12, which) -> {
//                        // Do nothing here because we override this button later
//                        // to change the close behaviour. However, we still need
//                        // this because on older versions of Android unless we
//                        // pass a handler the button doesn't get instantiated
//                    })
//            .setNegativeButton(R.string.action_cancel,
//                    (dialog1, which) -> {
//                        // Do nothing.
//                    })
//            .create();
//        dialog.show();
//        final Context con = this;
//
//        // Override/define behavior for positive button click.
//        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
//                v -> {
//                    // Re-Init mDumpWithPos in order to remove unwanted sectors.
//                    initDumpWithPosFromDump(dump);
//                    for (CheckBox box : sectorBoxes) {
//                        int sector = Integer.parseInt(box.getTag().toString());
//                        if (!box.isChecked()) {
//                            mDumpWithPos.remove(sector);
//                        }
//                    }
//                    if (mDumpWithPos.isEmpty()) {
//                        // Error. There is nothing to write.
//                        Toast.makeText(context, R.string.info_nothing_to_write,
//                                Toast.LENGTH_LONG).show();
//                        return;
//                    }
//
//                    // Check if last sector is out of range.
//                    if (!isSectorInRage(con, false)) {
//                        return;
//                    }
//
//                    // Create key map.
//                    createKeyMapForDump();
//                    dialog.dismiss();
//                });
    }

    /**
     * Check if the chosen sector or last sector of a dump is in the
     * range of valid sectors (according to {@link Preferences}).
     * @param context The context in error messages are displayed.
     * @return True if the sector is in range, False if not. Also,
     * if there was no tag False will be returned.
     */
    private boolean isSectorInRage(Context context, boolean isWriteBlock) {
//        MCReader reader = Common.checkForTagAndCreateReader(this);
//        if (reader == null) {
//            return false;
//        }
//        int lastValidSector = reader.getSectorCount() - 1;
//        int lastSector;
//        reader.close();
//        // Initialize last sector.
//        if (isWriteBlock) {
//            lastSector = Integer.parseInt(
//                    mSectorTextBlock.getText().toString());
//        } else {
//            lastSector = Collections.max(mDumpWithPos.keySet());
//        }
//
//        // Is last sector in range?
//        if (lastSector > lastValidSector) {
//            // Error. Tag too small for dump.
//            Toast.makeText(context, R.string.info_tag_too_small,
//                    Toast.LENGTH_LONG).show();
//            reader.close();
//            return false;
//        }
        return true;
    }

    /**
     * Initialize  with the data from a dump.
     * Transform the simple dump array into a structure (mDumpWithPos)
     * where the sector and block information are known additionally.
     * Blocks containing unknown data ("-") are dropped.
     * @param dump The dump to initialize the mDumpWithPos with.
     */
    private void initDumpWithPosFromDump(String[] dump) {
//        mDumpWithPos = new HashMap<>();
//        int sector = 0;
//        int block = 0;
//        // Transform the simple dump array into a structure (mDumpWithPos)
//        // where the sector and block information are known additionally.
//        // Blocks containing unknown data ("-") are dropped.
//        for (int i = 0; i < dump.length; i++) {
//            if (dump[i].startsWith("+")) {
//                String[] tmp = dump[i].split(": ");
//                sector = Integer.parseInt(tmp[tmp.length-1]);
//                block = 0;
//                mDumpWithPos.put(sector, new HashMap<>());
//            } else if (!dump[i].contains("-")) {
//                // Use static Access Conditions for all sectors?
//                if (mEnableStaticAC.isChecked()
//                        && (i+1 == dump.length || dump[i+1].startsWith("+"))) {
//                    // This is a Sector Trailer. Replace its ACs
//                    // with the static ones.
//                    String newBlock = dump[i].substring(0, 12)
//                            + mStaticAC.getText().toString()
//                            + dump[i].substring(18);
//                    dump[i] = newBlock;
//                }
//                mDumpWithPos.get(sector).put(block++,
//                        Common.hex2Bytes(dump[i]));
//            } else {
//                block++;
//            }
//        }
    }

    /**
     * Create a key map for the dump .
     * @see KeyMapCreator
     */
    private void createKeyMapForDump() {
//        // Show key map creator.
//        Intent intent = new Intent(this, KeyMapCreator.class);
//        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
//                Common.getFile(Common.KEYS_DIR).getAbsolutePath());
//        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
//        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM,
//                (int) Collections.min(mDumpWithPos.keySet()));
//        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_TO,
//                (int) Collections.max(mDumpWithPos.keySet()));
//        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
//                getString(R.string.action_create_key_map_and_write_dump));
//        startActivityForResult(intent, CKM_WRITE_DUMP);
    }

    /**
     * Check if the tag is suitable for the dump
     * This is done in four steps. The first check determines if the dump
     * fits on the tag (size check). The second check determines if the keys for
     * relevant sectors are known (key check). The third check determines if
     * specail blocks (block 0 and sector trailers) are correct. At last this
     * method will check whether the keys with write privileges are known and if
     * some blocks are read-only (write check).<br />
     * If some of these checks "fail", the user will get a report dialog
     * with the two options to cancel the whole write process or to
     * write as much as possible(call {@link #writeDump(HashMap,
     * SparseArray)}).
     * @see MCReader#isWritableOnPositions(HashMap, SparseArray)
     * @see Common#getOperationRequirements(byte, byte,
     * byte, Common.Operation, boolean, boolean)
     * @see #writeDump(HashMap, SparseArray)
     */
    private void checkDumpAgainstTag() {
//        // Create reader.
//        MCReader reader = Common.checkForTagAndCreateReader(this);
//        if (reader == null) {
//            Toast.makeText(this, R.string.info_tag_lost_check_dump,
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // Check if tag is correct size for dump.
//        if (reader.getSectorCount()-1 < Collections.max(
//                mDumpWithPos.keySet())) {
//            // Error. Tag too small for dump.
//            Toast.makeText(this, R.string.info_tag_too_small,
//                    Toast.LENGTH_LONG).show();
//            reader.close();
//            return;
//        }
//
//        // Check if tag is writable on needed blocks.
//        // Reformat for reader.isWritableOnPosition(...).
//        final SparseArray<byte[][]> keyMap  =
//                Common.getKeyMap();
//        HashMap<Integer, int[]> dataPos =
//                new HashMap<>(mDumpWithPos.size());
//        for (int sector : mDumpWithPos.keySet()) {
//            int i = 0;
//            int[] blocks = new int[mDumpWithPos.get(sector).size()];
//            for (int block : mDumpWithPos.get(sector).keySet()) {
//                blocks[i++] = block;
//            }
//            dataPos.put(sector, blocks);
//        }
//        HashMap<Integer, HashMap<Integer, Integer>> writeOnPos =
//                reader.isWritableOnPositions(dataPos, keyMap);
//        reader.close();
//
//        if (writeOnPos == null) {
//            // Error while checking for keys with write privileges.
//            Toast.makeText(this, R.string.info_tag_lost_check_dump,
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // Skip dialog:
//        // Build a dialog showing all sectors and blocks containing data
//        // that can not be overwritten with the reason why they are not
//        // writable. The user can chose to skip all these blocks/sectors
//        // or to cancel the whole write procedure.
//        List<HashMap<String, String>> list = new
//                ArrayList<>();
//        final HashMap<Integer, HashMap<Integer, Integer>> writeOnPosSafe =
//                new HashMap<>(
//                        mDumpWithPos.size());
//
//        // Check for keys that are missing completely (mDumpWithPos vs. keyMap).
//        HashSet<Integer> sectors = new HashSet<>();
//        for (int sector : mDumpWithPos.keySet()) {
//            if (keyMap.indexOfKey(sector) < 0) {
//                // Problem. Keys for sector not found.
//                addToList(list, getString(R.string.text_sector) + ": " + sector,
//                        getString(R.string.text_keys_not_known));
//            } else {
//                sectors.add(sector);
//            }
//        }
//
//        // Check for keys with write privileges that are missing (writeOnPos vs. keyMap).
//        // Check for blocks (block-parts) that are read-only.
//        // Check for issues of block 0 of the dump about to be written.
//        // Check the Access Conditions of the dump about to be written.
//        for (int sector : sectors) {
//            if (writeOnPos.get(sector) == null) {
//                // Error. Sector is dead (IO Error) or ACs are invalid.
//                addToList(list, getString(R.string.text_sector) + ": " + sector,
//                        getString(R.string.text_invalid_ac_or_sector_dead));
//                continue;
//            }
//            byte[][] keys = keyMap.get(sector);
//            Set<Integer> blocks = mDumpWithPos.get(sector).keySet();
//            for (int block : blocks) {
//                boolean isSafeForWriting = true;
//                String position = getString(R.string.text_sector) + ": "
//                        + sector + ", " + getString(R.string.text_block)
//                        + ": " + block;
//
//                // Special block 0 checks.
//                if (!mWriteManufBlock.isChecked()
//                        && sector == 0 && block == 0) {
//                    // Block 0 is read-only. This is normal. Skip.
//                    continue;
//                } else if (mWriteManufBlock.isChecked()
//                        && sector == 0 && block == 0) {
//                    // Block 0 should be written. Check it.
//                    String block0 = Common.bytes2Hex(mDumpWithPos.get(0).get(0));
//                    int block0Check = checkBlock0(block0, false);
//                    switch (block0Check) {
//                        case 1:
//                            Toast.makeText(this, R.string.info_tag_lost_check_dump,
//                                    Toast.LENGTH_LONG).show();
//                            return;
//                        case 2:
//                            // BCC not valid. Abort.
//                            Toast.makeText(this, R.string.info_bcc_not_valid,
//                                    Toast.LENGTH_LONG).show();
//                            return;
//                        case 3:
//                            addToList(list, position, getString(
//                                    R.string.text_block0_warning));
//                            break;
//                    }
//                }
//
//                // Special Access Conditions checks.
//                if ((sector < 31 && block == 3) || sector >= 31 && block == 15) {
//                    String sectorTrailer = Common.bytes2Hex(
//                            mDumpWithPos.get(sector).get(block));
//                    int acCheck = checkAccessConditions(sectorTrailer, false);
//                    switch (acCheck) {
//                        case 1:
//                            // Access Conditions not valid. Abort.
//                            Toast.makeText(this, R.string.info_ac_format_error,
//                                    Toast.LENGTH_LONG).show();
//                            return;
//                        case 2:
//                            addToList(list, position, getString(
//                                    R.string.info_irreversible_acs));
//                            break;
//                    }
//                }
//
//                // Normal write privileges checks.
//                int writeInfo = writeOnPos.get(sector).get(block);
//                switch (writeInfo) {
//                case 0:
//                    // Problem. Block is read-only.
//                    addToList(list, position, getString(
//                            R.string.text_block_read_only));
//                    isSafeForWriting = false;
//                    break;
//                case 1:
//                    if (keys[0] == null) {
//                        // Problem. Key with write privileges (A) not known.
//                        addToList(list, position, getString(
//                                R.string.text_write_key_a_not_known));
//                        isSafeForWriting = false;
//                    }
//                    break;
//                case 2:
//                    if (keys[1] == null) {
//                        // Problem. Key with write privileges (B) not known.
//                        addToList(list, position, getString(
//                                R.string.text_write_key_b_not_known));
//                        isSafeForWriting = false;
//                    }
//                    break;
//                case 3:
//                    // No Problem. Both keys have write privileges.
//                    // Set to key A or B depending on which one is available.
//                    writeInfo = (keys[0] != null) ? 1 : 2;
//                    break;
//                case 4:
//                    if (keys[0] == null) {
//                        // Problem. Key with write privileges (A) not known.
//                        addToList(list, position, getString(
//                                R.string.text_write_key_a_not_known));
//                        isSafeForWriting = false;
//                    } else {
//                        // Problem. ACs are read-only.
//                        addToList(list, position, getString(
//                                R.string.text_ac_read_only));
//                    }
//                    break;
//                case 5:
//                    if (keys[1] == null) {
//                        // Problem. Key with write privileges (B) not known.
//                        addToList(list, position, getString(
//                                R.string.text_write_key_b_not_known));
//                        isSafeForWriting = false;
//                    } else {
//                        // Problem. ACs are read-only.
//                        addToList(list, position, getString(
//                                R.string.text_ac_read_only));
//                    }
//                    break;
//                case 6:
//                    if (keys[1] == null) {
//                        // Problem. Key with write privileges (B) not known.
//                        addToList(list, position, getString(
//                                R.string.text_write_key_b_not_known));
//                        isSafeForWriting = false;
//                    } else {
//                        // Problem. Keys are read-only.
//                        addToList(list, position, getString(
//                                R.string.text_keys_read_only));
//                    }
//                    break;
//                case -1:
//                    // Error. Some strange error occurred. Maybe due to some
//                    // corrupted ACs...
//                    addToList(list, position, getString(
//                            R.string.text_strange_error));
//                    isSafeForWriting = false;
//                }
//                // Add if safe for writing.
//                if (isSafeForWriting) {
//                    if (writeOnPosSafe.get(sector) == null) {
//                        // Create sector.
//                        HashMap<Integer, Integer> blockInfo =
//                                new HashMap<>();
//                        blockInfo.put(block, writeInfo);
//                        writeOnPosSafe.put(sector, blockInfo);
//                    } else {
//                        // Add to sector.
//                        writeOnPosSafe.get(sector).put(block, writeInfo);
//                    }
//                }
//            }
//        }
//
//        // Show skip/cancel dialog (if needed).
//        if (!list.isEmpty()) {
//            // If the user skips all sectors/blocks that are not writable,
//            // the writeTag() method will be called.
//            LinearLayout ll = new LinearLayout(this);
//            int pad = Common.dpToPx(5);
//            ll.setPadding(pad, pad, pad, pad);
//            ll.setOrientation(LinearLayout.VERTICAL);
//            TextView textView = new TextView(this);
//            textView.setText(R.string.dialog_write_issues);
//            textView.setPadding(0,0,0, Common.dpToPx(5));
//            TextViewCompat.setTextAppearance(textView,
//                    android.R.style.TextAppearance_Medium);
//            ListView listView = new ListView(this);
//            ll.addView(textView);
//            ll.addView(listView);
//            String[] from = new String[] {"position", "reason"};
//            int[] to = new int[] {android.R.id.text1, android.R.id.text2};
//            ListAdapter adapter = new SimpleAdapter(this, list,
//                    android.R.layout.two_line_list_item, from, to);
//            listView.setAdapter(adapter);
//
//            new AlertDialog.Builder(this)
//                .setTitle(R.string.dialog_write_issues_title)
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setView(ll)
//                .setPositiveButton(R.string.action_skip_blocks,
//                        (dialog, which) -> {
//                            // Skip not writable blocks and start writing.
//                            writeDump(writeOnPosSafe, keyMap);
//                        })
//                .setNegativeButton(R.string.action_cancel_all,
//                        (dialog, which) -> {
//                            // Do nothing.
//                        })
//                .show();
//        } else {
//            // Write.
//            writeDump(writeOnPosSafe, keyMap);
//        }
    }

    /**
     * A helper function for {@link #checkDumpAgainstTag()} adding an item to
     * the list of all blocks with write issues.
     * This list will be displayed to the user in a dialog before writing.
     * @param list The list in which to add the key-value-pair.
     * @param position The key (position) for the list item
     * (e.g. "Sector 2, Block 3").
     * @param reason The value (reason) for the list item
     * (e.g. "Block is read-only").
     */
    private void addToList(List<HashMap<String, String>> list,
                           String position, String reason) {
//        HashMap<String, String> item = new HashMap<>();
//        item.put( "position", position);
//        item.put( "reason", reason);
//        list.add(item);
    }

    /**
     * This method is triggered by {@link #checkDumpAgainstTag()} and writes a dump
     * to a tag.
     * @param writeOnPos A map within a map (all with type = Integer).
     * The key of the outer map is the sector number and the value is another
     * map with key = block number and value = write information. The write
     * information must be filtered (by {@link #checkDumpAgainstTag()}) return values
     * of {@link MCReader#isWritableOnPositions(HashMap, SparseArray)}.<br />
     * Attention: This method does not any checking. The position and write
     * information must be checked by {@link #checkDumpAgainstTag()}.
     * @param keyMap A key map generated by {@link KeyMapCreator}.
     */
    private void writeDump(
        final HashMap<Integer, HashMap<Integer, Integer>> writeOnPos,
        final SparseArray<byte[][]> keyMap) {
//        // Check for write data.
//        if (writeOnPos.isEmpty()) {
//            // Nothing to write. Exit.
//            Toast.makeText(this, R.string.info_nothing_to_write,
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        // Create reader.
//        final MCReader reader = Common.checkForTagAndCreateReader(this);
//        if (reader == null) {
//            return;
//        }
//
//        // Display don't remove warning.
//        LinearLayout ll = new LinearLayout(this);
//        int pad = Common.dpToPx(20);
//        ll.setPadding(pad, pad, pad, pad);
//        ll.setGravity(Gravity.CENTER);
//        ProgressBar progressBar = new ProgressBar(this);
//        progressBar.setIndeterminate(true);
//        pad = Common.dpToPx(20);
//        progressBar.setPadding(0, 0, pad, 0);
//        TextView tv = new TextView(this);
//        tv.setText(getString(R.string.dialog_wait_write_tag));
//        tv.setTextSize(18);
//        ll.addView(progressBar);
//        ll.addView(tv);
//        final AlertDialog warning = new AlertDialog.Builder(this)
//            .setTitle(R.string.dialog_wait_write_tag_title)
//            .setView(ll)
//            .create();
//        warning.show();
//
//
//        // Start writing in new thread.
//        final Activity a = this;
//        final Handler handler = new Handler(Looper.getMainLooper());
//        new Thread(() -> {
//            // Write dump to tag.
//            for (int sector : writeOnPos.keySet()) {
//                byte[][] keys = keyMap.get(sector);
//                for (int block : writeOnPos.get(sector).keySet()) {
//                    // Select key with write privileges.
//                    byte[] writeKey = null;
//                    boolean useAsKeyB = true;
//                    int wi = writeOnPos.get(sector).get(block);
//                    if (wi == 1 || wi == 4) {
//                        writeKey = keys[0]; // Write with key A.
//                        useAsKeyB = false;
//                    } else if (wi == 2 || wi == 5 || wi == 6) {
//                        writeKey = keys[1]; // Write with key B.
//                    }
//
//                    // Write block.
//                    // Writing multiple blocks consecutively sometimes fails. I have no idea why.
//                    // It also depends on the data (see: https://github.com/ikarus23/MifareClassicTool/issues/412).
//                    // This makes no sense. This error does not occurs while debugging which
//                    // might indicate a timing issue. When adding a delay of 200 ms, the error
//                    // does not occur. Retrying to write also works. This why the ugly workaround
//                    // of trying to write at least two times was added.
//                    int result = 0;
//                    for (int i = 0; i < 2; i++) {
//                        result = reader.writeBlock(sector, block,
//                            mDumpWithPos.get(sector).get(block),
//                            writeKey, useAsKeyB);
//                        if (result == 0) {
//                            break;
//                        }
//                    }
//
//                    if (result != 0) {
//                        // Error. Some error while writing.
//                        handler.post(() -> Toast.makeText(a,
//                                R.string.info_write_error,
//                                Toast.LENGTH_LONG).show());
//                        reader.close();
//                        warning.cancel();
//                        return;
//                    }
//                }
//            }
//            // Finished writing.
//            reader.close();
//            warning.cancel();
//            handler.post(() -> Toast.makeText(a, R.string.info_write_successful,
//                    Toast.LENGTH_LONG).show());
//            a.finish();
//        }).start();
    }

    /**
     * Open the clone UID tool.
     * @param view The View object that triggered the method
     * (in this case the clone UID button).
     * @see KeyMapCreator
     */
    public void onCloneUid(View view) {
        // Show the clone UID tool.
        Intent intent = new Intent(this, CloneUidTool.class);
        startActivity(intent);
    }

    /**
     * Open key map creator.
     * @param view The View object that triggered the method
     * (in this case the factory format button).
     * @see KeyMapCreator
     */
    public void onFactoryFormat(View view) {
        // Show key map creator.
        Intent intent = new Intent(this, KeyMapCreator.class);
        intent.putExtra(KeyMapCreator.EXTRA_KEYS_DIR,
            Common.getFile(Common.KEYS_DIR).getAbsolutePath());
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER, false);
        intent.putExtra(KeyMapCreator.EXTRA_SECTOR_CHOOSER_FROM, -1);
        intent.putExtra(KeyMapCreator.EXTRA_BUTTON_TEXT,
            getString(R.string.action_create_key_map_and_factory_format));
        startActivityForResult(intent, CKM_FACTORY_FORMAT);
    }

    /**
     * Create an factory formatted, empty dump with a size matching
     * the current tag size and then call {@link #checkDumpAgainstTag()}.
     * Factory (default) MIFARE Classic Access Conditions are: 0xFF0780XX
     * XX = General purpose byte (GPB): Most of the time 0x69. At the end of
     * an Tag XX = 0xBC.
     * @see #checkDumpAgainstTag()
     */
    private void createFactoryFormattedDump() {
//        // This function is directly called after a key map was created.
//        // So Common.getTag() will return den current present tag
//        // (and its size/sector count).
//        mDumpWithPos = new HashMap<>();
//        int sectors = MifareClassic.get(Common.getTag()).getSectorCount();
//        byte[] emptyBlock = new byte[]
//                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        byte[] normalSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
//                -1, 7, -128, 105, -1, -1, -1, -1, -1, -1};
//        byte[] lastSectorTrailer = new byte[] {-1, -1, -1, -1, -1, -1,
//                -1, 7, -128, -68, -1, -1, -1, -1, -1, -1};
//        // Empty 4 block sector.
//        HashMap<Integer, byte[]> empty4BlockSector =
//                new HashMap<>(4);
//        for (int i = 0; i < 3; i++) {
//            empty4BlockSector.put(i, emptyBlock);
//        }
//        empty4BlockSector.put(3, normalSectorTrailer);
//        // Empty 16 block sector.
//        HashMap<Integer, byte[]> empty16BlockSector =
//                new HashMap<>(16);
//        for (int i = 0; i < 15; i++) {
//            empty16BlockSector.put(i, emptyBlock);
//        }
//        empty16BlockSector.put(15, normalSectorTrailer);
//        // Last sector.
//        HashMap<Integer, byte[]> lastSector;
//
//        // Sector 0.
//        HashMap<Integer, byte[]> firstSector =
//                new HashMap<>(4);
//        firstSector.put(1, emptyBlock);
//        firstSector.put(2, emptyBlock);
//        firstSector.put(3, normalSectorTrailer);
//        mDumpWithPos.put(0, firstSector);
//        // Sector 1 - (max.) 31.
//        for (int i = 1; i < sectors && i < 32; i++) {
//            mDumpWithPos.put(i, empty4BlockSector);
//        }
//        // Sector 32 - 39.
//        if (sectors == 40) {
//            // Add the large sectors (containing 16 blocks)
//            // of a MIFARE Classic 4k tag.
//            for (int i = 32; i < sectors && i < 39; i++) {
//                mDumpWithPos.put(i, empty16BlockSector);
//            }
//            // In the last sector the Sector Trailer is different.
//            lastSector = new HashMap<>(empty16BlockSector);
//            lastSector.put(15, lastSectorTrailer);
//        } else {
//            // In the last sector the Sector Trailer is different.
//            lastSector = new HashMap<>(empty4BlockSector);
//            lastSector.put(3, lastSectorTrailer);
//        }
//        mDumpWithPos.put(sectors - 1, lastSector);
//        checkDumpAgainstTag();
    }

    /**
     * Check the user input and (if correct) show the
     * {@link KeyMapCreator} with predefined mapping range
     * (see {@link #createKeyMapForBlock(int, boolean)}).
     * After a key map was created {@link #writeValueBlock()} will be triggered.
     * @param view The View object that triggered the method
     * (in this case the write Value Block button).
     * @see KeyMapCreator
     * @see #checkSectorAndBlock(EditText,
     * EditText)
     */
    public void onWriteValue(View view) {
//        // Check input.
//        if (!checkSectorAndBlock(mSectorTextVB, mBlockTextVB)) {
//            return;
//        }
//
//        int sector = Integer.parseInt(mSectorTextVB.getText().toString());
//        int block = Integer.parseInt(mBlockTextVB.getText().toString());
//        if (block == 3 || block == 15 || (sector == 0 && block == 0)) {
//            // Error. Block can't be a Value Block.
//            Toast.makeText(this, R.string.info_not_vb,
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        try {
//            Integer.parseInt(mNewValueTextVB.getText().toString());
//        } catch (Exception e) {
//            // Error. Value is too big.
//            Toast.makeText(this, R.string.info_value_too_big,
//                    Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        createKeyMapForBlock(sector, true);
    }

    /**
     * Called from {@link #onActivityResult(int, int, Intent)}
     * after a key map was created, this method tries to increment or
     * decrement the Value Block. Possible errors are displayed to the
     * user via Toast.
     * @see #onActivityResult(int, int, Intent)
     * @see #onWriteValue(View)
     */
    private void writeValueBlock() {
//        // Write the new value (incr./decr. + transfer).
//        MCReader reader = Common.checkForTagAndCreateReader(this);
//        if (reader == null) {
//            return;
//        }
//        int value = Integer.parseInt(mNewValueTextVB.getText().toString());
//        int sector = Integer.parseInt(mSectorTextVB.getText().toString());
//        int block = Integer.parseInt(mBlockTextVB.getText().toString());
//        byte[][] keys = Common.getKeyMap().get(sector);
//        int result = -1;
//
//        if (keys[1] != null) {
//            result = reader.writeValueBlock(sector, block, value,
//                    mIncreaseVB.isChecked(),
//                    keys[1], true);
//        }
//        // Error while writing? Maybe tag has default factory settings ->
//        // try to write with key a (if there is one).
//        if (result == -1 && keys[0] != null) {
//            result = reader.writeValueBlock(sector, block, value,
//                    mIncreaseVB.isChecked(),
//                    keys[0], false);
//        }
//        reader.close();
//
//        // Error handling.
//        switch (result) {
//            case 2:
//                Toast.makeText(this, R.string.info_block_not_in_sector,
//                        Toast.LENGTH_LONG).show();
//                return;
//            case -1:
//                Toast.makeText(this, R.string.info_error_writing_value_block,
//                        Toast.LENGTH_LONG).show();
//                return;
//        }
//        Toast.makeText(this, R.string.info_write_successful,
//                Toast.LENGTH_LONG).show();
//        finish();
    }


    /**
     * Copy the standard key files ({@link Common#STD_KEYS} and
     * {@link Common#STD_KEYS_EXTENDED}) form assets to {@link Common#KEYS_DIR}.
     * @see Common#KEYS_DIR
     * @see Common#HOME_DIR
     * @see Common#copyFile(InputStream, OutputStream)
     */
    private void copyStdKeysFiles() {
        AssetManager assetManager = getAssets();
        try {
            for (String file : assetManager.list(Common.KEYS_DIR)) {
                String filePath = Common.KEYS_DIR + "/" + file;
                InputStream in = assetManager.open(filePath);
                OutputStream out = new FileOutputStream(Common.getFile(filePath));
                Common.copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            }
        } catch(IOException e) {
            Log.e(LOG_TAG, "Error while copying files from assets.");
        }
    }
}

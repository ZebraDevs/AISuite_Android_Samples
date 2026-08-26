package com.zebra.aisuite_quickstart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RadioButton;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zebra.aisuite_quickstart.databinding.ActivitySettingsBinding;
import com.zebra.aisuite_quickstart.utils.CommonUtils;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean resOptions=false;
    private boolean modelSizeOptions=false;
    private boolean aboutOptions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.status_bar_color));
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // We get the system status bar height and add it as top padding to the root layout
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainSettings, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.settingsToolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
        }

        binding.settingsToolbar.setNavigationOnClickListener(v -> finish());
        sharedPreferences = getSharedPreferences(CommonUtils.SETTINGS_PREFS, MODE_PRIVATE);
        // Obtain the SharedPreferences editor
        editor = sharedPreferences.edit();

        int modelInputSize = sharedPreferences.getInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 640);
        if (modelInputSize == 640) {
            binding.rb640.setChecked(true);
        } else if (modelInputSize == 1280) {
            binding.rb1280.setChecked(true);
        } else if (modelInputSize == 1600) {
            binding.rb1600.setChecked(true);
        }else if (modelInputSize == 2560) {
            binding.rb2560.setChecked(true);
        }

        String resolution = sharedPreferences.getString(CommonUtils.PREF_RESOLUTION, "2MP");
        switch (resolution) {
            case "1MP":
                binding.rb1MP.setChecked(true);
                break;
            case "2MP":
                binding.rb2MP.setChecked(true);
                break;
            case "4MP":
                binding.rb4MP.setChecked(true);
                break;
            case "8MP":
                binding.rb8MP.setChecked(true);
                break;
        }

        binding.radioGroupModelSize.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);

            if (binding.rb640 == selectedRadioButton) {
                editor.putInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 640);
                editor.apply();
            } else if (binding.rb1280 == selectedRadioButton) {
                editor.putInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 1280);
                editor.apply();
            } else if (binding.rb1600 == selectedRadioButton) {
                editor.putInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 1600);
                editor.apply();
            } else if (binding.rb2560 == selectedRadioButton) {
                editor.putInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 2560);
                editor.apply();
            }
        });

        binding.ivModelSize.setOnClickListener(v -> {
            modelSizeOptions = !modelSizeOptions;
            if (modelSizeOptions) {
                binding.llModelInputSize.setVisibility(View.VISIBLE);
                binding.ivModelSize.setImageResource(R.drawable.arrow_up);
            } else {
                binding.llModelInputSize.setVisibility(View.GONE);
                binding.ivModelSize.setImageResource(R.drawable.arrow_down);
            }
        });

        binding.radioGroupResolution.setOnCheckedChangeListener((group, checkedId) -> {
            RadioButton selectedRadioButton = findViewById(checkedId);

            if (binding.rb1MP == selectedRadioButton) {
                editor.putString(CommonUtils.PREF_RESOLUTION, "1MP");
                editor.apply();
            } else if (binding.rb2MP == selectedRadioButton) {
                editor.putString(CommonUtils.PREF_RESOLUTION, "2MP");
                editor.apply();
            } else if (binding.rb4MP == selectedRadioButton) {
                editor.putString(CommonUtils.PREF_RESOLUTION, "4MP");
                editor.apply();
            }else if (binding.rb8MP == selectedRadioButton) {
                editor.putString(CommonUtils.PREF_RESOLUTION, "8MP");
                editor.apply();
            }
        });

        binding.ivResolution.setOnClickListener(v -> {
            resOptions = !resOptions;
            if (resOptions) {
                binding.llResol.setVisibility(View.VISIBLE);
                binding.ivResolution.setImageResource(R.drawable.arrow_up);
            } else {
                binding.llResol.setVisibility(View.GONE);
                binding.ivResolution.setImageResource(R.drawable.arrow_down);
            }
        });

        setAboutVersions();

        binding.aboutHeader.setOnClickListener(v -> toggleAboutSection());
        binding.ivAbout.setOnClickListener(v -> toggleAboutSection());
    }
    private void setAboutVersions() {
        binding.tvSdkVersion.setText(
                getString(R.string.sdk_version_format, BuildConfig.AI_VISION_SDK_VERSION)
        );

        binding.tvBarcodeDecoderVersion.setText(
                getString(R.string.barcode_decoder_version_format, BuildConfig.BARCODE_DECODER_VERSION)
        );

        binding.tvTextOcrVersion.setText(
                getString(R.string.text_ocr_version_format, BuildConfig.TEXT_OCR_VERSION)
        );

        binding.tvModuleRecognizerVersion.setText(
                getString(R.string.module_recognizer_version_format, BuildConfig.MODULE_RECOGNIZER_VERSION)
        );

        binding.tvPalletBoxLocalizerVersion.setText(
                getString(R.string.pallet_box_localizer_version_format, BuildConfig.PALLET_BOX_LOCALIZER_VERSION)
        );
    }

    private void toggleAboutSection() {
        aboutOptions = !aboutOptions;

        if (aboutOptions) {
            binding.llAbout.setVisibility(View.VISIBLE);
            binding.ivAbout.setImageResource(R.drawable.arrow_up);
        } else {
            binding.llAbout.setVisibility(View.GONE);
            binding.ivAbout.setImageResource(R.drawable.arrow_down);
        }
    }
}
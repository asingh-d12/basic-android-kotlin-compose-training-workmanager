/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.bluromatic.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bluromatic.R
import com.example.bluromatic.data.BlurAmount
import com.example.bluromatic.ui.theme.BluromaticTheme

@Composable
fun BluromaticScreen(blurViewModel: BlurViewModel = viewModel(factory = BlurViewModel.Factory)) {
    val uiState by blurViewModel.blurUiState.collectAsStateWithLifecycle()
    BluromaticTheme {
        BluromaticScreenContent(
            blurUiState = uiState,
            blurAmountOptions = blurViewModel.blurAmount,
            applyBlur = blurViewModel::applyBlur,
            cancelWork = {
                blurViewModel.cancelWork()
            }
        )
    }
}

@Composable
fun BluromaticScreenContent(
    blurUiState: BlurUiState,
    blurAmountOptions: List<BlurAmount>,
    applyBlur: (Int) -> Unit,
    cancelWork: () -> Unit
) {
    val context = LocalContext.current

    var selectedValue by rememberSaveable { mutableStateOf(1) }

    val permissionsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        arrayOf()
    }

    var notifyPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    var storeImageToStorage by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mutableStateOf(true)
        } else {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->

            permissionsToCheck.forEach {
                if (permissions[it] == true) {

                    if (it == Manifest.permission.POST_NOTIFICATIONS) {
                        notifyPermission = true
                    }

                    if(it == Manifest.permission.WRITE_EXTERNAL_STORAGE){
                        storeImageToStorage = true
                    }

                    // Here run the process once permission is granted
                    if(notifyPermission && storeImageToStorage) {
                        applyBlur(selectedValue)
                    }

                } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                        context as Activity,
                        it
                    )
                ) {
                    Toast.makeText(
                        context,
                        "Permission $it required to provide this functionality",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }


        }
    )



    Column(
        modifier = Modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Image(
            painter = painterResource(R.drawable.android_cupcake),
            contentDescription = stringResource(R.string.description_image),
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            contentScale = ContentScale.Fit,
        )
        BlurAmountContent(
            selectedValue = selectedValue,
            blurAmounts = blurAmountOptions,
            onSelectedValueChange = { selectedValue = it }
        )
        BlurActions(
            blurUiState = blurUiState,
            onGoClick = {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Check for notification permission if Android API Level 33+
                    if (!notifyPermission) {
                        launcher.launch(permissionsToCheck)
                    }
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if(!storeImageToStorage){
                        launcher.launch(permissionsToCheck)
                    }
                }

                // I haven't added any check to not run applyBlur if permission is not granted
                if (notifyPermission && storeImageToStorage) {
                    applyBlur(selectedValue)
                } else {
                    Toast.makeText(
                        context,
                        "Need Notification Permission to run this",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSeeFileClick = { currentUri ->
                showBlurredImage(context = context, currentUri = currentUri)
            },
            onCancelClick = { cancelWork() }
        )
    }
}

@Composable
private fun BlurActions(
    blurUiState: BlurUiState,
    onGoClick: () -> Unit,
    onSeeFileClick: (String) -> Unit,
    onCancelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {

        println("Currently blurUiState = $blurUiState")

        when(blurUiState){
            is BlurUiState.Default -> {
                // This is default state
                Button(onGoClick) { Text(stringResource(R.string.go)) }
            }

            is BlurUiState.Loading -> {
                Button(onCancelClick) { Text(stringResource(R.string.cancel_work)) }
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }

            is BlurUiState.Complete -> {
                Button(onGoClick) { Text(stringResource(R.string.go)) }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { onSeeFileClick(blurUiState.outputUri) }) {
                    Text(text = stringResource(id = R.string.see_file))
                }
            }
        }

    }
}

@Composable
private fun BlurAmountContent(
    selectedValue: Int,
    blurAmounts: List<BlurAmount>,
    modifier: Modifier = Modifier,
    onSelectedValueChange: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
    ) {
        Text(
            text = stringResource(R.string.blur_title),
            style = MaterialTheme.typography.h5
        )
        blurAmounts.forEach { amount ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        role = Role.RadioButton,
                        selected = (selectedValue == amount.blurAmount),
                        onClick = { onSelectedValueChange(amount.blurAmount) }
                    )
                    .size(48.dp)
            ) {
                RadioButton(
                    selected = (selectedValue == amount.blurAmount),
                    onClick = null,
                    modifier = Modifier.size(48.dp),
                    colors = RadioButtonDefaults.colors(
                        selectedColor = MaterialTheme.colors.primary
                    )
                )
                Text(stringResource(amount.blurAmountRes))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BluromaticScreenContentPreview() {
    BluromaticTheme {
        BluromaticScreenContent(
            blurUiState = BlurUiState.Default,
            blurAmountOptions = listOf(BlurAmount(R.string.blur_lv_1, 1)),
            {},
            {}
        )
    }
}

private fun showBlurredImage(context: Context, currentUri: String) {
    val uri = if (currentUri.isNotEmpty()) {
        Uri.parse(currentUri)
    } else {
        null
    }

    val actionView = Intent(Intent.ACTION_VIEW, uri).apply {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q){
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            // Without adding type I get - "exposed beyond app through Intent.getData()"
            type = "image/png"
        }
    }
    context.startActivity(actionView)
}

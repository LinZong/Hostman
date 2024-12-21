package moe.nemesiss.hostman.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString
import moe.nemesiss.hostman.R
import moe.nemesiss.hostman.boost.EasyDebug

private const val IPV4 = "IPV4"
private const val IPV6 = "IPV6"
private const val TAG = "EditHostEntryDialog"
private val ipAddressOptions = listOf(IPV4, IPV6)

sealed class HostNameValidationResult
data object HostNameValid : HostNameValidationResult()
data class HostNameInvalid(val message: String) : HostNameValidationResult()

sealed class IPAddressValidationResult
data object IPAddressValid : IPAddressValidationResult()
data class IPAddressInvalid(val message: String) : IPAddressValidationResult()

data class HostEntryValidationResult(val hostNameValidationResult: HostNameValidationResult,
                                     val ipAddressValidationResult: IPAddressValidationResult) {
    val isValid get() = hostNameValidationResult is HostNameValid && ipAddressValidationResult is IPAddressValid
}


private interface HostEntryValidator {
    fun validate(hostName: String, hostAddress: String): HostEntryValidationResult
}

private object IPV4HostEntryValidator : HostEntryValidator {
    override fun validate(hostName: String, hostAddress: String): HostEntryValidationResult {
        return HostEntryValidationResult(
            hostNameValidationResult = if (hostName.isEmpty() && hostAddress.isNotEmpty()) HostNameInvalid("Host name should not be empty") else HostNameValid,
            ipAddressValidationResult = validateIPV4Address(hostAddress)
        )
    }

    private fun validateIPV4Address(address: String): IPAddressValidationResult {
        try {
            if (address.isEmpty()) {
                return IPAddressInvalid("Address should not be empty")
            }
            IPAddressString(address).toAddress(IPAddress.IPVersion.IPV4)
            return IPAddressValid
        } catch (t: Throwable) {
            return IPAddressInvalid("Invalid IPV4 address: $address")
        }
    }
}


private object IPV6HostEntryValidator : HostEntryValidator {


    override fun validate(hostName: String, hostAddress: String): HostEntryValidationResult {
        return HostEntryValidationResult(
            hostNameValidationResult = if (hostName.isEmpty() && hostAddress.isNotEmpty()) HostNameInvalid("Host name should not be empty") else HostNameValid,
            ipAddressValidationResult = validateIPV6Address(hostAddress)
        )
    }


    private fun validateIPV6Address(address: String): IPAddressValidationResult {
        try {
            if (address.isEmpty()) {
                return IPAddressInvalid("Address should not be empty")
            }
            IPAddressString(address).toAddress(IPAddress.IPVersion.IPV6)
            return IPAddressValid
        } catch (t: Throwable) {
            return IPAddressInvalid("Invalid IPV6 address: $address")
        }
    }
}

fun interface HostEntryEditConfirmation {
    /**
     * Callback method while user is confirming a host entry create/edit action.
     */
    fun onConfirmation(previousEntry: HostEntry?, newEntry: HostEntry)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHostEntryDialog(
    entry: HostEntry? = null,
    dismiss: () -> Unit = {},
    confirmation: HostEntryEditConfirmation = HostEntryEditConfirmation { _, _ -> }
) {
    val editing = entry != null
    var ipAddressTypeText by remember { mutableStateOf(TextFieldValue(text = if (entry?.ipv6 == true) IPV6 else IPV4)) }
    val ipFamily = if (ipAddressTypeText.text == IPV6) IPAddress.IPVersion.IPV6 else IPAddress.IPVersion.IPV4
    val validator = if (ipFamily == IPAddress.IPVersion.IPV6) IPV6HostEntryValidator else IPV4HostEntryValidator
    val validation: HostEntryValidationResult


    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val title = if (editing) "Edit host entry" else "Create a new host entry"

    var hostName by remember {
        mutableStateOf(entry?.hostName ?: "")
    }

    var hostAddress by remember {
        mutableStateOf(entry?.address?.hostAddress ?: "")
    }


    validation = validator.validate(hostName, hostAddress)

    val enableIpAddressTypeSelection = !editing


    AlertDialog(
        onDismissRequest = {
            dismiss()
        },
        confirmButton = {
            TextButton(onClick = {
                if (validation.isValid) {
                    IPAddressString(hostAddress).toAddress(ipFamily)?.let { ipAddress ->
                        val hostEntry = HostEntry(hostName, ipAddress.toInetAddress())
                        confirmation.onConfirmation(hostEntry, hostEntry)
                    } ?: run {
                        EasyDebug.error(TAG) { "Failed to convert hostAddress: $hostAddress to IPAddress in family: $ipFamily through validation was passed, method returns null." }
                    }
                }
            }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                dismiss()
            }) {
                Text(stringResource(R.string.cancel))
            }
        },
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = {
                        if (enableIpAddressTypeSelection) {
                            setExpanded(it)
                        }
                    },
                ) {
                    OutlinedTextField(
                        // The `menuAnchor` modifier must be passed to the text field to handle
                        // expanding/collapsing the menu on click. An editable text field has
                        // the anchor type `PrimaryEditable`.
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable,
                                                       enabled = enableIpAddressTypeSelection),
                        value = ipAddressTypeText,
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("IP Address Type") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = expanded, // If the text field is editable, it is recommended to make the
                                // trailing icon a `menuAnchor` of type `SecondaryEditable`. This
                                // provides a better experience for certain accessibility services
                                // to choose a menu option without typing.
                                modifier = Modifier.menuAnchor(MenuAnchorType.SecondaryEditable),
                            )
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { setExpanded(false) },
                    ) {
                        ipAddressOptions.forEach { optionText ->
                            DropdownMenuItem(
                                text = { Text(text = optionText, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    ipAddressTypeText =
                                        TextFieldValue(text = optionText, selection = TextRange(optionText.length))
                                    setExpanded(false)
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                val hostNameError = validation.hostNameValidationResult is HostNameInvalid
                Column {
                    OutlinedTextField(value = hostName,
                                      onValueChange = {
                                          hostName = it
                                      },
                                      singleLine = true,
                                      leadingIcon = {
                                          Icon(painter = painterResource(id = R.drawable.link_24dp_5f6368_fill0_wght400_grad0_opsz24),
                                               contentDescription = stringResource(R.string.host_name))
                                      },
                                      label = { Text(text = stringResource(id = R.string.host_name)) },
                                      isError = hostNameError
                    )

                    // Use hand-crafted supportingText instead of built-in one
                    // in order to prevent from losing TextField focus while supportingText is appearing or disappearing.
                    if (hostNameError) {
                        Text(
                            text = (validation.hostNameValidationResult as HostNameInvalid).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                val hostAddressError =
                    hostAddress.isNotEmpty() && validation.ipAddressValidationResult is IPAddressInvalid

                Column {
                    OutlinedTextField(value = hostAddress,
                                      onValueChange = {
                                          hostAddress = it
                                      },
                                      singleLine = true,
                                      leadingIcon = {
                                          Icon(painter = painterResource(id = R.drawable.dns_24dp_5f6368_fill0_wght400_grad0_opsz24),
                                               contentDescription = stringResource(R.string.host_address))
                                      },
                                      label = { Text(text = stringResource(R.string.host_address)) },
                                      keyboardOptions = if (ipAddressTypeText.text == IPV4) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                                      isError = hostAddressError
                    )

                    if (hostAddressError) {
                        Text(
                            text = (validation.ipAddressValidationResult as IPAddressInvalid).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        })
}

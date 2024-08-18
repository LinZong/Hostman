package moe.nemesiss.hostman.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressString
import moe.nemesiss.hostman.R

private const val IPV4 = "IPV4"
private const val IPV6 = "IPV6"
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
            hostNameValidationResult = if (hostName.isEmpty()) HostNameInvalid("Host name should not be empty") else HostNameValid,
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
            hostNameValidationResult = if (hostName.isEmpty()) HostNameInvalid("Host name should not be empty") else HostNameValid,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditHostEntryDialog(
    entry: HostEntry? = null,
    onDismissRequest: () -> Unit = {},
    onConfirmation: (HostEntry) -> Unit = {},
) {
    val editing = entry != null
    var ipAddressTypeText by remember { mutableStateOf(TextFieldValue(text = if (entry?.ipv6 == true) IPV6 else IPV4)) }
    val ipFamily = if (ipAddressTypeText.text == IPV6) IPAddress.IPVersion.IPV6 else IPAddress.IPVersion.IPV4
    val validator = if (ipFamily == IPAddress.IPVersion.IPV6) IPV6HostEntryValidator else IPV4HostEntryValidator
    var validation by remember { mutableStateOf(HostEntryValidationResult(HostNameValid, IPAddressValid)) }


    val (expanded, setExpanded) = remember { mutableStateOf(false) }
    val title = if (editing) "Edit host entry" else "Create a new host entry"

    var hostName by remember {
        mutableStateOf(entry?.hostName ?: "")
    }

    var hostAddress by remember {
        mutableStateOf(entry?.address?.hostAddress ?: "")
    }


    LaunchedEffect(key1 = hostName, key2 = hostAddress) {
        validation = validator.validate(hostName, hostAddress)
    }

    val enableIpAddressTypeSelection = !editing


    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(onClick = {
                if (validation.isValid) {
                    onConfirmation(HostEntry(hostName,
                                             IPAddressString(hostAddress).toAddress(ipFamily).toInetAddress()))
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest()
            }) {
                Text("Cancel")
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
                        supportingText = {},
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

                OutlinedTextField(value = hostName,
                                  onValueChange = {
                                      hostName = it
                                  },
                                  singleLine = true,
                                  leadingIcon = {
                                      Icon(painter = painterResource(id = R.drawable.link_24dp_5f6368_fill0_wght400_grad0_opsz24),
                                           contentDescription = "Host Name")
                                  },
                                  label = { Text(text = "Host Name") },
                                  supportingText = {
                                      if (validation.hostNameValidationResult is HostNameInvalid) {
                                          Text(text = (validation.hostNameValidationResult as HostNameInvalid).message)
                                      }
                                  },
                                  isError = hostName.isNotEmpty() && validation.hostNameValidationResult is HostNameInvalid
                )

                OutlinedTextField(value = hostAddress,
                                  onValueChange = {
                                      hostAddress = it
                                  },
                                  singleLine = true,
                                  leadingIcon = {
                                      Icon(painter = painterResource(id = R.drawable.dns_24dp_5f6368_fill0_wght400_grad0_opsz24),
                                           contentDescription = "Host Address")
                                  },
                                  label = { Text(text = "Host Address") },
                                  keyboardOptions = if (ipAddressTypeText.text == IPV4) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
                                  supportingText = {
                                      if (hostAddress.isNotEmpty() && validation.ipAddressValidationResult is IPAddressInvalid) {
                                          Text(text = (validation.ipAddressValidationResult as IPAddressInvalid).message)
                                      }
                                  },
                                  isError = hostAddress.isNotEmpty() && validation.ipAddressValidationResult is IPAddressInvalid
                )
            }
        })
}

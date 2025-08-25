package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.BuildConfig
import com.zebra.aidatacapturedemo.R

@Composable
fun AboutScreen(innerPadding: PaddingValues) {
    var showDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(color = Variables.surfaceDefault)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 12.dp)
                .align(Alignment.TopStart)
        ) {
            // Row 1
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .background(color = Variables.mainLight)
            ) {
                Text(
                    text = "About",

                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Variables.mainDefault,
                    ),
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Row 2
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .background(color = Variables.mainInverse)
            ) {
                Text(
                    text = "Explore Zebra Mobile Computing AI Suite Data Capture SDK latest features and solutions for Zebra Android™ devices.",

                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.mainDefault,
                    ),
                    modifier = Modifier.padding(
                        start = 12.dp,
                        end = 12.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    )
                )
            }

            // Row 3
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Data Capture Demo",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Color(0xFF1D1E23),
                    ),
                    modifier = Modifier.padding(top = 10.dp, start = 14.4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                val appVersion = BuildConfig.AI_DataCaptureDemo_Version
                Text(
                    text = appVersion,

                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF646A78),
                        textAlign = TextAlign.Right,
                    ),
                    modifier = Modifier.padding(end = 22.4.dp, top = 14.dp)
                )
            }

            // Row 4
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Suite SDK",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Color(0xFF1D1E23),
                    ),
                    modifier = Modifier.padding(top = 16.dp, start = 14.4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                val aisdkVersion = BuildConfig.Zebra_AI_VisionSdk_Version
                Text(
                    text = aisdkVersion,

                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF646A78),
                        textAlign = TextAlign.Right,
                    ),
                    modifier = Modifier.padding(top = 20.dp, end = 22.4.dp)
                )
            }

            // Row 5
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "End User License Agreement",

                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                        fontWeight = FontWeight(500),
                        color = Color(0xFF1D1E23),
                    ),
                    modifier = Modifier.padding(start = 16.dp, top = 30.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        showDialog = true
                    },
                    modifier = Modifier.padding(top = 30.dp, end = 12.dp)
                ) {
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.icon_arrow_forward),
                        contentDescription = "Arrow Button",
                        tint = Variables.mainDefault
                    )
                }
            }

            // Row 6
            Row(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Copyright © 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.",
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Color(0xFF1D1E23),
                    ),
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 36.dp,
                        bottom = 28.dp
                    )
                )
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(
                            text = "End User License Agreement (Restricted Software)",

                            // Standard/Title Large
                            style = TextStyle(
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                                fontWeight = FontWeight(500),
                                color = Color(0xFF1D1E23),
                                textAlign = TextAlign.Center,
                            )
                        )
                    },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            val eULAHtmlText = "<br>\n" +
                                    "    This End User License Agreement (this “Agreement”) includes important information about your\n" +
                                    "    relationship with Zebra. Please read it carefully.\n" +
                                    "    <br><br>\n" +
                                    "    1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Introduction</u>\n" +
                                    "    <br>\n" +
                                    "    1.1&nbsp;&nbsp;&nbsp;&nbsp; This Agreement is a legal contract made between the person or entity\n" +
                                    "    agreeing to these terms and conditions (“you”) and Zebra Technologies Corporation (“Zebra”) that\n" +
                                    "    governs your use of software, firmware, application programming interfaces, user interfaces, and any\n" +
                                    "    other type of machine-readable instructions or code as provided by Zebra that accompany or reference\n" +
                                    "    this Agreement, along with any corresponding documentation (collectively, the “Software”).\n" +
                                    "    <br><br>\n" +
                                    "    1.2&nbsp;&nbsp;&nbsp;&nbsp; By ordering, subscribing to, installing, executing, or otherwise using\n" +
                                    "    the Software, you (i) acknowledge that you have read and understand this Agreement, (ii) agree to be\n" +
                                    "    bound by this Agreement, (iii) confirm that you are lawfully able to enter into contracts, and (iv)\n" +
                                    "    if you are accepting this Agreement on behalf of an entity, such as an organization or business,\n" +
                                    "    confirm that you have the authority to bind that entity to this Agreement.\n" +
                                    "    <br><br>\n" +
                                    "    2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Term of this Agreement</u>\n" +
                                    "    <br>\n" +
                                    "    2.1&nbsp;&nbsp;&nbsp;&nbsp; This Agreement becomes effective on the earlier of (i) the date you\n" +
                                    "    accept this Agreement by, for example, clicking a button and (ii) the earliest date on which you\n" +
                                    "    install, execute, or otherwise use the Software, and ends upon termination in accordance with this\n" +
                                    "    section (“Term”).\n" +
                                    "    <br><br>\n" +
                                    "    2.2&nbsp;&nbsp;&nbsp;&nbsp; This Agreement will automatically terminate without notice from Zebra\n" +
                                    "    upon your breach or violation of any term or condition of this Agreement.\n" +
                                    "    <br><br>\n" +
                                    "    2.3&nbsp;&nbsp;&nbsp;&nbsp; If you are in possession of the Software pursuant to a subscription\n" +
                                    "    model or other type of commercial agreement, this Agreement shall terminate upon the expiration or\n" +
                                    "    termination of that subscription model or other type of commercial agreement.\n" +
                                    "    <br><br>\n" +
                                    "    2.4&nbsp;&nbsp;&nbsp;&nbsp; Upon termination of this Agreement, you will immediately cease using the\n" +
                                    "    Software and delete (i) the Software, (ii) any other application provided to you by Zebra for\n" +
                                    "    purposes of interacting with the Software, and (iii) any Zebra Content (as defined below) obtained\n" +
                                    "    through your use of the Software.\n" +
                                    "    <br><br>\n" +
                                    "    2.5&nbsp;&nbsp;&nbsp;&nbsp; You may terminate this Agreement by ceasing all use of the Software and\n" +
                                    "    deleting the Software from your devices.\n" +
                                    "    <br><br>\n" +
                                    "    2.6&nbsp;&nbsp;&nbsp;&nbsp; Sections 4, 6, 8, and 11-14 will survive the termination or expiration\n" +
                                    "    of this Agreement.\n" +
                                    "    <br><br>\n" +
                                    "    3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>License and Ownership</u>\n" +
                                    "    <br>\n" +
                                    "    3.1&nbsp;&nbsp;&nbsp;&nbsp; Subject to your compliance with this Agreement, Zebra grants you a\n" +
                                    "    limited, revocable, non-exclusive, non-sublicensable license to, during the Term, use the Software\n" +
                                    "    solely for your internal business purposes and, for Software delivered with Zebra hardware, solely\n" +
                                    "    in support of Zebra hardware.\n" +
                                    "    <br><br>\n" +
                                    "    3.2&nbsp;&nbsp;&nbsp;&nbsp; The Software is licensed; not sold. Zebra reserves all right, title, and\n" +
                                    "    interest not expressly granted to you in this Agreement. Zebra or its licensors or suppliers own the\n" +
                                    "    title, copyright, and other intellectual property rights in the Software and certain Content\n" +
                                    "    associated therewith.\n" +
                                    "    <br><br>\n" +
                                    "    4&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Restrictions</u>\n" +
                                    "    <br>\n" +
                                    "    4.1&nbsp;&nbsp;&nbsp;&nbsp; You shall not or permit another to modify, distribute, publicly display,\n" +
                                    "    publicly perform, or create derivative works of the Software.\n" +
                                    "    <br><br>\n" +
                                    "    4.2&nbsp;&nbsp;&nbsp;&nbsp; You shall not or permit another to disassemble, decompile,\n" +
                                    "    reverse-engineer, or attempt to discover or derive the source code of the Software, except and only\n" +
                                    "    to the extent that such activity is expressly permitted by applicable law not withstanding this\n" +
                                    "    limitation.\n" +
                                    "    <br><br>\n" +
                                    "    4.3&nbsp;&nbsp;&nbsp;&nbsp; You shall not or permit another to rent, sell, lease, lend, sublicense,\n" +
                                    "    provide commercial hosting services involving the Software, or in any other way allow third parties\n" +
                                    "    to exploit the Software.\n" +
                                    "    <br><br>\n" +
                                    "    4.4&nbsp;&nbsp;&nbsp;&nbsp; You shall not or permit another to modify, circumvent, deactivate,\n" +
                                    "    degrade or thwart any software-based or hardware-based protection mechanism Zebra has in place to\n" +
                                    "    safeguard the Software.\n" +
                                    "    <br><br>\n" +
                                    "    4.5&nbsp;&nbsp;&nbsp;&nbsp; The rights granted to you hereunder are associated with you and cannot\n" +
                                    "    be used or otherwise applied to anyone other than you. Unless made in connection with a sale of a\n" +
                                    "    device on which the Software is installed by or under the authorization of Zebra, you may not convey\n" +
                                    "    the Software to any third-party or permit any third party to do so.\n" +
                                    "    <br><br>\n" +
                                    "    4.6&nbsp;&nbsp;&nbsp;&nbsp; You may not assign this Agreement or any rights or obligations\n" +
                                    "    hereunder, by operation of law or otherwise, without prior written consent from Zebra. Zebra may\n" +
                                    "    assign this Agreement and its rights and obligations without your consent. Subject to the foregoing,\n" +
                                    "    this Agreement shall be binding upon and inure to the benefit of the parties to it and their\n" +
                                    "    respective legal representatives, successors, and permitted assigns.\n" +
                                    "    <br><br>\n" +
                                    "    5&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Zebra’s Approach to Privacy</u>\n" +
                                    "    <br>\n" +
                                    "    5.1&nbsp;&nbsp;&nbsp;&nbsp; Zebra’s Privacy Policy (located at: <a\n" +
                                    "        href=\"https://www.zebra.com/privacy\">https://www.zebra.com/privacy</a>), as amended from\n" +
                                    "    time to time, is hereby incorporated by reference into this Agreement. If you submit personal data\n" +
                                    "    to Zebra in connection with your use of the Software, the ways in which Zebra collects and uses that\n" +
                                    "    data are regulated by Zebra’s Privacy Policy in accordance with applicable law.\n" +
                                    "    <br><br>\n" +
                                    "    5.2&nbsp;&nbsp;&nbsp;&nbsp; Zebra is committed to General Data Protection Regulation (GDPR)\n" +
                                    "    compliance and Zebra’s GDPR Addendum (located at: <a\n" +
                                    "        href=\"https://www.zebra.com/GDPR\">https://www.zebra.com/GDPR</a>\n" +
                                    "    supplements Zebra’s Privacy Policy.\n" +
                                    "    <br><br>\n" +
                                    "    6&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Permissions</u>\n" +
                                    "    <br>\n" +
                                    "    6.1&nbsp;&nbsp;&nbsp;&nbsp; “Content” means image data, images, graphics, text, templates, formats,\n" +
                                    "    forms, digital certificates or other types of user-identifying packages, plug-ins, widgets, audio,\n" +
                                    "    video, and audiovisual data.\n" +
                                    "    <br><br>\n" +
                                    "    6.2&nbsp;&nbsp;&nbsp;&nbsp; “Input” means data provided to Zebra, whether by you or another person\n" +
                                    "    using the Software, for use by the Software to provide a feature or functionality. Input includes\n" +
                                    "    Content, measurement values, readings, sensor outputs, calculation results, and instructions.\n" +
                                    "    <br><br>\n" +
                                    "    6.3&nbsp;&nbsp;&nbsp;&nbsp; You acknowledge that if the Software requires access to non-Zebra\n" +
                                    "    hardware, non-Zebra software, or non-Zebra Content to perform a function or provide a feature and\n" +
                                    "    you deny such permission, the corresponding function or feature will not be available or execute\n" +
                                    "    properly.\n" +
                                    "    <br><br>\n" +
                                    "    6.4&nbsp;&nbsp;&nbsp;&nbsp; Certain functions of the Software may require access to certain software\n" +
                                    "    and/or hardware. To the extent permission is required, you hereby grant Zebra permission to, during\n" +
                                    "    the Term, access all software incorporated into Zebra hardware as necessary for the Software to\n" +
                                    "    perform those functions.\n" +
                                    "    <br><br>\n" +
                                    "    6.5&nbsp;&nbsp;&nbsp;&nbsp; You agree that any ideas, suggestions, comments, or reviews you provide\n" +
                                    "    to Zebra in relation to the Software (“Feedback”) is not confidential, and Zebra shall not have any\n" +
                                    "    obligation to treat Feedback as confidential information. You agree that Zebra is free to use\n" +
                                    "    Feedback to improve its products and services.\n" +
                                    "    <br><br>\n" +
                                    "    6.6&nbsp;&nbsp;&nbsp;&nbsp; Where applicable, you agree to waive and not enforce any “moral rights”\n" +
                                    "    or equivalent rights you have in Feedback, Input, or your Content provided to Zebra in connection\n" +
                                    "    with the Software.\n" +
                                    "    <br><br>\n" +
                                    "    7&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Updates and Fixes</u>\n" +
                                    "    <br>\n" +
                                    "    7.1&nbsp;&nbsp;&nbsp;&nbsp; Nothing in this Agreement entitles you to new releases of the Software.\n" +
                                    "    If Zebra, at its discretion, makes updates, fixes, or patches to the Software available during the\n" +
                                    "    Term without providing superseding terms, this Agreement applies to such updates, fixes, and\n" +
                                    "    patches.\n" +
                                    "    <br><br>\n" +
                                    "    7.2&nbsp;&nbsp;&nbsp;&nbsp; Provided that the functionality and features of the Software remain\n" +
                                    "    substantially similar thereafter, Zebra may automatically update the Software without requiring your\n" +
                                    "    acceptance. Zebra will make reasonable efforts to provide you notice of any automatic updates made\n" +
                                    "    to the Software, although such notice is not required.\n" +
                                    "    <br><br>\n" +
                                    "    8&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Data Collection</u>\n" +
                                    "    <br>\n" +
                                    "    8.1&nbsp;&nbsp;&nbsp;&nbsp; “Anonymized Data” means data that cannot be used to identify you or any\n" +
                                    "    other person.\n" +
                                    "    <br><br>\n" +
                                    "    8.2&nbsp;&nbsp;&nbsp;&nbsp; “Pseudonymized Data” means data that cannot be used to identify you or\n" +
                                    "    any other person without the use of additional information that is kept separately and is subject to\n" +
                                    "    technical and organizational measures to ensure that the personal data is not attributed to you or\n" +
                                    "    any other person.\n" +
                                    "    <br><br>\n" +
                                    "    8.3&nbsp;&nbsp;&nbsp;&nbsp; You acknowledge and agree that Zebra may, as permitted by law:\n" +
                                    "    <br><br>\n" +
                                    "    8.3.1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; collect Pseudonymized Data associated with your use\n" +
                                    "    of the Software, including data generated by the Software and/or data generated by any device\n" +
                                    "    incorporating software that interfaces with the Software;\n" +
                                    "    <br><br>\n" +
                                    "    8.3.2&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; create aggregated data records using the\n" +
                                    "    Pseudonymized Data;\n" +
                                    "    <br><br>\n" +
                                    "    8.3.3&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; use the aggregated data records to improve the\n" +
                                    "    Software, develop new software or services, understand industry trends, create and publish white\n" +
                                    "    papers, reports, or databases summarizing the foregoing, investigate and help address and/or prevent\n" +
                                    "    actual or potential unlawful activity, and generally for any legitimate purpose related to Zebra’s\n" +
                                    "    business; and\n" +
                                    "    <br><br>\n" +
                                    "    8.3.4&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; retain Pseudonymized Data as Anonymized Data when\n" +
                                    "    you delete the Software.\n" +
                                    "    <br><br>\n" +
                                    "    8.4&nbsp;&nbsp;&nbsp;&nbsp; “Machine Data” means usage or status information collected by the\n" +
                                    "    Software or hardware that interfaces with the Software, such as information related to a computing\n" +
                                    "    device running the Software. Example machine data includes remaining usage time, network information\n" +
                                    "    (e.g., name or identifier), wireless signal strength, device identifier, software version, hardware\n" +
                                    "    version, device type, metadata associated with the operation of the Software, LED state, reboot\n" +
                                    "    cause, storage and memory availability or usage, power cycle count, and device up time.\n" +
                                    "    <br><br>\n" +
                                    "    8.5&nbsp;&nbsp;&nbsp;&nbsp; The Software may provide Machine Data to Zebra.\n" +
                                    "    <br><br>\n" +
                                    "    8.6&nbsp;&nbsp;&nbsp;&nbsp; All title and ownership rights in and to Machine Data are held by Zebra.\n" +
                                    "    In the event, and to the extent you are deemed to have any ownership rights in Machine Data, you\n" +
                                    "    hereby grant Zebra a perpetual, irrevocable, fully paid, royalty free, worldwide license to use\n" +
                                    "    Machine Data.\n" +
                                    "    <br><br>\n" +
                                    "    9&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Modifications of this Agreement</u>\n" +
                                    "    <br>\n" +
                                    "    Modification or amendment of this Agreement must be made through written agreement by authorized\n" +
                                    "    representatives of each party. Written agreement may be satisfied by Zebra’s offer of a superseding\n" +
                                    "    agreement for your use of the Software and your acceptance thereof by clicking a button presented\n" +
                                    "    with the superseding agreement or use of the Software and your acceptance thereof by clicking a\n" +
                                    "    button presented with the superseding agreement or using the Software after being presented with the\n" +
                                    "    superseding agreement.\n" +
                                    "    <br><br>\n" +
                                    "    10&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Third-Party Content</u>\n" +
                                    "    <br>\n" +
                                    "    10.1&nbsp;&nbsp;&nbsp;&nbsp; The Software may include a link to a third-party resource that makes\n" +
                                    "    third-party Content or services available for purchase and/or download from the corresponding\n" +
                                    "    third-party.\n" +
                                    "    <br><br>\n" +
                                    "    10.2&nbsp;&nbsp;&nbsp;&nbsp; Access to and use of third-party Content or services is subject to\n" +
                                    "    terms and conditions provided by the third-party and may be protected by the third-party’s copyright\n" +
                                    "    or other intellectual property rights. Nothing in this Agreement is a license, permission, or\n" +
                                    "    assignment of any rights in or to third-party Content or services.\n" +
                                    "    <br><br>\n" +
                                    "    10.3&nbsp;&nbsp;&nbsp;&nbsp; Third-party resources linked or made available via the Software are not\n" +
                                    "    considered part of the Software and Zebra may disable integrations of third-party Content or\n" +
                                    "    compatibility of the Software with third-party Content at Zebra’s discretion.\n" +
                                    "    <br><br>\n" +
                                    "    11&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>DISCLAIMERS OF WARRANTY AND\n" +
                                    "        LIMITATIONS OF LIABILITY</u>\n" +
                                    "    <br>\n" +
                                    "    11.1&nbsp;&nbsp;&nbsp;&nbsp; THE SOFTWARE IS PROVIDED \"AS IS\" AND ON AN \"AS AVAILABLE\" BASIS. TO THE\n" +
                                    "    FULLEST EXTENT POSSIBLE PURSUANT TO APPLICABLE LAW, ZEBRA DISCLAIMS ALL WARRANTIES, EXPRESS,\n" +
                                    "    IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, IMPLIED WARRANTIES OF MERCHANTABILITY,\n" +
                                    "    SATISFACTORY QUALITY OR WORKMANLIKE EFFORT, FITNESS FOR A PARTICULAR PURPOSE, RELIABILITY OR\n" +
                                    "    AVAILABILITY, ACCURACY, LACK OF VIRUSES, NON-INFRINGEMENT OF THIRD-PARTY RIGHTS OR OTHER VIOLATION\n" +
                                    "    OF RIGHTS. ZEBRA DOES NOT WARRANT THAT THE OPERATION OR AVAILABILITY OF THE SOFTWARE WILL BE\n" +
                                    "    UNINTERRUPTED OR ERROR FREE. NO ADVICE OR INFORMATION, WHETHER ORAL OR WRITTEN, OBTAINED BY YOU FROM\n" +
                                    "    ZEBRA OR ITS AFFILIATES SHALL BE DEEMED TO ALTER THIS DISCLAIMER OF WARRANTY REGARDING THE SOFTWARE\n" +
                                    "    OR TO CREATE ANY WARRANTY OF ANY SORT FROM ZEBRA. SOME JURISDICTIONS DO NOT ALLOW EXCLUSIONS OR\n" +
                                    "    LIMITATIONS OF IMPLIED WARRANTIES, SO SOME OF THE EXCLUSIONS OR LIMITATIONS OF THIS SECTION MAY NOT\n" +
                                    "    APPLY TO YOU.\n" +
                                    "    <br><br>\n" +
                                    "    11.2&nbsp;&nbsp;&nbsp;&nbsp; CERTAIN THIRD-PARTY CONTENT MAY BE INCORPORATED WITH OR ACCESSIBLE VIA\n" +
                                    "    THE SOFTWARE. ZEBRA MAKES NO REPRESENTATIONS WHATSOEVER ABOUT ANY THIRD-PARTY CONTENT. SINCE ZEBRA\n" +
                                    "    HAS LIMITED OR NO CONTROL OVER SUCH CONTENT, YOU ACKNOWLEDGE AND AGREE THAT ZEBRA IS NOT RESPONSIBLE\n" +
                                    "    FOR SUCH CONTENT. YOU EXPRESSLY ACKNOWLEDGE AND AGREE THAT USE OF THIRD-PARTY CONTENT IS AT YOUR\n" +
                                    "    SOLE RISK AND THAT THE ENTIRE RISK OF UNSATISFACTORY QUALITY, PERFORMANCE, ACCURACY, AND EFFORT IS\n" +
                                    "    WITH YOU. YOU AGREE THAT ZEBRA SHALL NOT BE RESPONSIBLE OR LIABLE, DIRECTLY OR INDIRECTLY, FOR ANY\n" +
                                    "    DAMAGE OR LOSS, INCLUDING BUT NOT LIMITED TO ANY DAMAGE TO OR LOSS OF DATA, CAUSED OR ALLEGED TO BE\n" +
                                    "    CAUSED BY, OR IN CONNECTION WITH, USE OF OR RELIANCE ON ANY THIRD-PARTY CONTENT AVAILABLE ON OR\n" +
                                    "    THROUGH THE SOFTWARE. YOU ACKNOWLEDGE AND AGREE THAT THE USE OF ANY THIRD-PARTY CONTENT IS GOVERNED\n" +
                                    "    BY THE THIRD-PARTY’S TERMS OF USE, LICENSE AGREEMENT, PRIVACY POLICY, OR OTHER SUCH AGREEMENT AND\n" +
                                    "    THAT ANY INFORMATION OR PERSONAL DATA YOU PROVIDE, WHETHER KNOWINGLY OR UNKNOWINGLY, TO THE\n" +
                                    "    THIRD-PARTY, WILL BE SUBJECT TO THE THIRD-PARTY’S PRIVACY POLICY, IF SUCH A POLICY EXISTS. ZEBRA\n" +
                                    "    DISCLAIMS ANY RESPONSIBILITY FOR ANY DISCLOSURE OF INFORMATION OR ANY OTHER PRACTICES OF ANY\n" +
                                    "    THIRD-PARTY. ZEBRA EXPRESSLY DISCLAIMS ANY WARRANTY REGARDING WHETHER YOUR PERSONAL INFORMATION IS\n" +
                                    "    CAPTURED BY ANY THIRD-PARTY OR THE USE TO WHICH SUCH PERSONAL INFORMATION MAY BE PUT BY SUCH\n" +
                                    "    THIRD-PARTY.\n" +
                                    "    <br><br>\n" +
                                    "    11.3&nbsp;&nbsp;&nbsp;&nbsp; IN NO EVENT WILL ZEBRA BE LIABLE TO YOU OR ANY OTHER THIRD-PARTY FOR\n" +
                                    "    ANY DAMAGES OF ANY KIND ARISING OUT OF OR RELATING TO THE USE OF OR ACCESS TO ANY COMPONENT OF THE\n" +
                                    "    SOFTWARE OR THE INABILITY TO USE OR ACCESS ANY COMPONENT OF THE SOFTWARE, INCLUDING BUT NOT LIMITED\n" +
                                    "    TO DAMAGES CAUSED BY OR RELATED TO ERRORS, OMISSIONS, INTERRUPTIONS, DEFECTS, DELAY IN OPERATION OR\n" +
                                    "    TRANSMISSION, COMPUTER VIRUS, FAILURE TO CONNECT, NETWORK CHARGES, IN-APP PURCHASES, AND ALL OTHER\n" +
                                    "    DIRECT, INDIRECT, SPECIAL, INCIDENTAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES EVEN IF ZEBRA HAS BEEN\n" +
                                    "    ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR\n" +
                                    "    LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THE ABOVE EXCLUSIONS OR LIMITATIONS MAY NOT\n" +
                                    "    APPLY TO YOU. NOTWITHSTANDING THE FOREGOING, ZEBRA’S TOTAL LIABILITY TO YOU FOR ALL LOSSES, DAMAGES,\n" +
                                    "    CAUSES OF ACTION, INCLUDING BUT NOT LIMITED TO THOSE BASED ON CONTRACT, TORT, OR OTHERWISE, ARISING\n" +
                                    "    OUT OF YOUR USE OF THE SOFTWARE OR ANY OTHER PROVISION UNDER THIS AGREEMENT, SHALL NOT EXCEED THE\n" +
                                    "    FAIR MARKET VALUE OF THAT COMPONENT OF THE SOFTWARE.\n" +
                                    "    <br><br>\n" +
                                    "    11.4&nbsp;&nbsp;&nbsp;&nbsp; THE FOREGOING LIMITATIONS, EXCLUSIONS, AND DISCLAIMERS HEREIN SHALL\n" +
                                    "    APPLY TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, EVEN IF ANY REMEDY FAILS ITS ESSENTIAL\n" +
                                    "    PURPOSE. \n" +
                                    "    <br><br>\n" +
                                    "    11.5&nbsp;&nbsp;&nbsp;&nbsp; THE SOFTWARE MAY ENABLE COLLECTION OF LOCATION-BASED DATA FROM ONE OR\n" +
                                    "    MORE DEVICES WHICH MAY ALLOW TRACKING OF THE LOCATION OF THOSE DEVICES. ZEBRA SPECIFICALLY DISCLAIMS\n" +
                                    "    ANY LIABILITY FOR YOUR USE OR MISUSE OF THE LOCATION-BASED DATA. YOU AGREE TO PAY ALL REASONABLE\n" +
                                    "    COSTS AND EXPENSES OF ZEBRA ARISING FROM OR RELATED TO THIRD-PARTY CLAIMS RESULTING FROM YOUR USE OR\n" +
                                    "    MISUSE OF THE LOCATION-BASED DATA.\n" +
                                    "    <br><br>\n" +
                                    "    12&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Third Party Claims</u>\n" +
                                    "    <br>\n" +
                                    "    In the event of a third-party claim against Zebra alleging that your (i) Content, (ii) Feedback, or\n" +
                                    "    (iii) Input infringes or misappropriates a third party’s intellectual property rights, you will\n" +
                                    "    defend and hold Zebra harmless against such a claim, provided Zebra gives you sufficient notice to\n" +
                                    "    fulfill your obligations of this Section 12 without prejudice due to Zebra’s delay.\n" +
                                    "    <br><br>\n" +
                                    "    13&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Governing Law</u>\n" +
                                    "    <br>\n" +
                                    "    This Agreement is governed by the laws of the State of Illinois, without regard to its conflict of\n" +
                                    "    law provisions. This Agreement shall not be governed by the UN Convention on Contracts for the\n" +
                                    "    International Sale of Goods, the application of which is expressly excluded. You hereby submit\n" +
                                    "    yourself and your property in any legal action or proceeding relating to this Agreement or for\n" +
                                    "    recognition and enforcement of any judgment in respect thereof to the exclusive general jurisdiction\n" +
                                    "    of the courts of the State of Illinois or to the United States North District Court of Illinois and\n" +
                                    "    to the respective appellate courts thereof in connection with any appeal therefrom.\n" +
                                    "    <br><br>\n" +
                                    "    14&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Handling of Disputes</u>\n" +
                                    "    <br>\n" +
                                    "    14.1&nbsp;&nbsp;&nbsp;&nbsp; You acknowledge that, in the event you breach any provision of this\n" +
                                    "    Agreement, Zebra may not have an adequate remedy in money or damages. Zebra shall therefore be\n" +
                                    "    entitled to seek an injunction against such breach from any court of competent jurisdiction\n" +
                                    "    immediately upon request without posting bond. Zebra's right to seek injunctive relief shall not\n" +
                                    "    limit its right to seek further remedies.\n" +
                                    "    <br><br>\n" +
                                    "    14.2&nbsp;&nbsp;&nbsp;&nbsp; If any term of this Agreement is to any extent illegal, otherwise\n" +
                                    "    invalid, or incapable of being enforced, such term shall be excluded to the extent of such\n" +
                                    "    invalidity or unenforceability, all other terms hereof shall remain in full force and effect, and,\n" +
                                    "    to the extent permitted and possible, the invalid or unenforceable term shall be deemed replaced by\n" +
                                    "    a term that is valid and enforceable and that comes closest to expressing the intention of such\n" +
                                    "    invalid or unenforceable term.\n" +
                                    "    <br><br>\n" +
                                    "    14.3&nbsp;&nbsp;&nbsp;&nbsp; You acknowledge that you and Zebra are the sole parties to this\n" +
                                    "    Agreement, and you agree to not seek remedies under this Agreement against Zebra’s authorized\n" +
                                    "    distributors or resellers with respect to the Software.\n" +
                                    "    <br><br>\n" +
                                    "    15&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>Open Source Software</u>\n" +
                                    "    <br>\n" +
                                    "    The Software may be subject to one or more open source licenses. The open source license provisions\n" +
                                    "    may override some terms of this Agreement. Zebra makes the applicable open source licenses available\n" +
                                    "    on a legal notices readme file and/or in system reference guides or in command line interface (CLI)\n" +
                                    "    reference guides associated with certain Zebra products.\n" +
                                    "    <br><br>\n" +
                                    "    16&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<u>U.S. Government End User Restricted\n" +
                                    "        Rights</u>\n" +
                                    "    <br>\n" +
                                    "    This provision only applies to U.S. Government end users. The Software is a “commercial item” as\n" +
                                    "    that term is defined at 48 C.F.R. Part 2.101, consisting of “commercial computer software” and\n" +
                                    "    “computer software documentation” as such terms are defined in 48 C.F.R. Part 252.227-7014(a)(1) and\n" +
                                    "    48 C.F.R. Part 252.227-7014(a)(5), and used in 48 C.F.R. Part 12.212 and 48 C.F.R. Part 227.7202, as\n" +
                                    "    applicable. Consistent with 48 C.F.R. Part 12.212, 48 C.F.R. Part 252.227-7015, 48 C.F.R. Part\n" +
                                    "    227.7202-1 through 227.7202-4, 48 C.F.R. Part 52.227-19, and other relevant sections of the Code of\n" +
                                    "    Federal Regulations, as applicable, the Software is distributed and licensed to U.S. Government end\n" +
                                    "    users (a) only as a commercial item, and (b) with only those rights as are granted to all other end\n" +
                                    "    users pursuant to the terms and conditions contained herein."
                            Text(
                                text = AnnotatedString.fromHtml(eULAHtmlText),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Color(0xFF636363),
                                    letterSpacing = 0.4.sp,
                                )
                            )
                        }
                    },
                    containerColor = Variables.surfaceDefault,
                    confirmButton = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { showDialog = false },
                                modifier = Modifier
                                    .height(40.dp)
                                    .fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Variables.mainPrimary
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                            {
                                Text(
                                    text = "Close",
                                    style = TextStyle(
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp,
                                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                        fontWeight = FontWeight(500),
                                        color = Variables.stateDefaultEnabled,
                                        textAlign = TextAlign.Center,
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
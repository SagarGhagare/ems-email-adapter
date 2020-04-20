package net.nhs.ems.emailadapter.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.mail.MessagingException;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNS;
import com.amazonaws.services.lambda.runtime.events.SNSEvent.SNSRecord;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceAsync;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import net.nhs.ems.emailadapter.service.OutgoingEmailBuilder;
import net.nhs.ems.emailadapter.service.StagedStopwatch;

@RunWith(PowerMockRunner.class)
@PrepareForTest(System.class)
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*", "javax.net.ssl.*"})
public class EmailHandlerTest {

  private final static String SENDER_KEY = "EMS_REPORT_SENDER";
  private final static String RECIPIENT_KEY = "EMS_REPORT_RECIPIENT";
  private final static String SUBJECT_KEY = "EMS_REPORT_SUBJECT";
  private final static String BODY_KEY = "EMS_REPORT_BODY";
  
  @Mock
  private AmazonS3 s3Client;
  
  @Mock
  private AmazonSimpleEmailServiceAsync ses;
  
  private EmailHandler emailHandler;
  
  @Mock
  private LambdaLogger lambdaLogger;

  @Mock
  private Context context;
  
  @Mock
  private S3Object s3Object;

  private S3ObjectInputStream content;
  private HttpRequestBase httpRequest;
  
  @Rule
  public final EnvironmentVariables environmentVariables = new EnvironmentVariables();
  
  @Before
  public void setup() throws FileNotFoundException {
    content = new S3ObjectInputStream(new FileInputStream(new File("src/test/resources/ch9jp4nhrfu2t82nui94eg7i8miejp9c11tunl81.txt")), httpRequest);
    Mockito.when(context.getLogger()).thenReturn(lambdaLogger);
    Mockito.when(s3Client.getObject(Mockito.anyString(), Mockito.anyString())).thenReturn(s3Object);
    Mockito.when(s3Object.getObjectContent()).thenReturn(content);
    Mockito.mock(OutgoingEmailBuilder.class);
    Mockito.mock(StagedStopwatch.class);
    emailHandler = new EmailHandler(s3Client, ses);
  }
  
  @Test
  public void testHandleRequest() throws MessagingException, ParseException {
    Map<String, String> map = new HashMap<String, String>();
    environmentVariables.set(SENDER_KEY, "abc@gmail.com");
    environmentVariables.set(RECIPIENT_KEY, "xyz@test.com");
    environmentVariables.set(SUBJECT_KEY, "test mail");
    environmentVariables.set(BODY_KEY, "This is sample mail");
    PowerMockito.mockStatic(System.class);
    PowerMockito.when(System.getenv()).thenReturn(map);
    
    emailHandler.handleRequest(createSnsRecord("arn:aws:sns:eu-west-1:410123189863:ems-receive-email-stgeorges", "Email for St Georges"), context);
  
    Mockito.verify(s3Client).getObject(Mockito.anyString(), Mockito.anyString());
    Mockito.verify(ses, Mockito.times(1)).sendRawEmail(Mockito.any(SendRawEmailRequest.class));
  }
  
  private SNSEvent createSnsRecord(String topicArn, String subject) {
    SNSEvent snsEvent = new SNSEvent();
    SNS sns = new SNS();
    sns.setTopicArn(topicArn);
    sns.setSubject(subject);
    sns.setMessage("{    \"notificationType\": \"Received\",    \"mail\": {        \"timestamp\": \"2020-04-17T15:59:39.639Z\",        \"source\": \"Sagar.Ghagare@mastek.com\",        \"messageId\": \"r6h454jgasdsdot7pgu2dmu5s404qiisn251k901\",        \"destination\": [            \"stgeorges@iucdspilot.uk\"        ],        \"headersTruncated\": false,        \"headers\": [            {                \"name\": \"Return-Path\",                \"value\": \"<Sagar.Ghagare@mastek.com>\"            },            {                \"name\": \"Received\",                \"value\": \"from APC01-HK2-obe.outbound.protection.outlook.com (mail-eopbgr1300072.outbound.protection.outlook.com [40.107.130.72]) by inbound-smtp.eu-west-1.amazonaws.com with SMTP id r6h454jgasdsdot7pgu2dmu5s404qiisn251k901 for stgeorges@iucdspilot.uk; Fri, 17 Apr 2020 15:59:39 +0000 (UTC)\"            },            {                \"name\": \"X-SES-Spam-Verdict\",                \"value\": \"PASS\"            },            {                \"name\": \"X-SES-Virus-Verdict\",                \"value\": \"PASS\"            },            {                \"name\": \"Received-SPF\",                \"value\": \"pass (spfCheck: domain of mastek.com designates 40.107.130.72 as permitted sender) client-ip=40.107.130.72; envelope-from=Sagar.Ghagare@mastek.com; helo=mail-eopbgr1300072.outbound.protection.outlook.com;\"            },            {                \"name\": \"Authentication-Results\",                \"value\": \"amazonses.com; spf=pass (spfCheck: domain of mastek.com designates 40.107.130.72 as permitted sender) client-ip=40.107.130.72; envelope-from=Sagar.Ghagare@mastek.com; helo=mail-eopbgr1300072.outbound.protection.outlook.com; dkim=pass header.i=@mastek.com; dmarc=pass header.from=mastek.com;\"            },            {                \"name\": \"X-SES-RECEIPT\",                \"value\": \"AEFBQUFBQUFBQUFISExmU2FpMnUzV2RHRkFNeWxrS3RKM1oxR0xBSXVUSjJydlpCN2FGZWpTenIwaTFOMjZEbEtCOGlZcFNnWk5oV2paZ25sTlpDWmxiZXRxT3RlTGhuTVdHWlRlVjRSNGF5anpXWjN1NHBSMUxvSEl4VDVyb1lwNERjWXRjQnluS0FlNytEM3ArUWJUaXFTeExldDdFb3FWcTgxTmFrZXFWM3RNT2crcEw1aWRwdnExQ0FjTkt5S2pVNHBRWlYvRTA5ZEcveVJhYXRjR2wvd1NkRVlLNXhweXNObUthWVJFTzhsYlo3ZDlXUElxZXBTaThhU2NiaUV5V01QMjArelpmc3ZBZ0xHL1hVZCtqeENCbzNESFhDRm1CVkVIM2Z0WHBYbk1USmFqeVpBZ2c9PQ==\"            },            {                \"name\": \"X-SES-DKIM-SIGNATURE\",                \"value\": \"a=rsa-sha256; q=dns/txt; b=0FrwSNGtAxO+2l/qNJM0Eud4Oei5oOTI3NxKgMOyo1GwvWZgv83XhFB95py8xk6qOaRoqMaYdytdEdkXOD5Ex1qaBnpOix732p2+lCS3uR7eFP3r19og055t3IZgoNf8p06zHIa7EVrpeYLE8ewMoL2f5L6V7AjioVS0u/guEJA=; c=relaxed/simple; s=shh3fegwg5fppqsuzphvschd53n6ihuv; d=amazonses.com; t=1587139180; v=1; bh=b0EX5S9IuMSEvHEBY8RNyDadH70ZZO6ymvTkZrdUzSE=; h=From:To:Cc:Bcc:Subject:Date:Message-ID:MIME-Version:Content-Type:X-SES-RECEIPT;\"            },            {                \"name\": \"ARC-Seal\",                \"value\": \"i=1; a=rsa-sha256; s=arcselector9901; d=microsoft.com; cv=none; b=mc8txiz/mbiAAUqDHmphvR+RlXY/ixcMaP10PWrAcRX0y9QZ5TwGwtMstBkuEqLTwDeHTs5d58AHBVU7sqbSQWk2h09s25Ab9v+xgxUQpGLZvKq4WcLuFJStC2zpS/rmUDoFRMLS/u2jORzklaOMMnrrs7I0CbD7GNV7FI1rNalDy/bIJElbBalriisrAjU970ETjCvl8UhI0UmZoT6Ss3xfK4/WEhGH4rRaLkakqE/Lu7rQwpM0vI6P3LwbwqLovFdgnuy1PYKoUoyEoNyXAm87tC+zuovHm6I1VyGCyEW/91x1DbN2woAJ0M09d7mQT38QKNfKoEIrzsLxPzRkhg==\"            },            {                \"name\": \"ARC-Message-Signature\",                \"value\": \"i=1; a=rsa-sha256; c=relaxed/relaxed; d=microsoft.com; s=arcselector9901; h=From:Date:Subject:Message-ID:Content-Type:MIME-Version:X-MS-Exchange-SenderADCheck; bh=j4NmNQnJpsyDil7EOOmYWCKQ+PTPtXEf+Qib3nn/+Fc=; b=EfdmKoUf23ZSGHrSqlqPgj8pNAgqOExhFV2inmNk0j0wylmHspQHF2OaDjrFUGLDC4cFaXQw4RfBYGuxpjhG3jlA/ZtUfU1qbXH2nyjSAFwjMvx1wE/zuhAbtSY37iUqiuVMo6Z8tdFwAhvC3Cf6YZZkbkiUHRBiCw39wKJvL6tykc4fRU+zplsjy/Aplygnh3wpaCBEnpmY0Zqj48dzNcPf6v4LP4w1oTa2xJApIqD0W4+lZH4Y9FQcNYPgo8uzC/dngenipCrsRCyZaBzS+1MJ2qMILNZM/+BfqluiZGSFBOHw4r6tCJxDDTfrfCUMWqiZ2rzZrnKO9Cpm2rZj+w==\"            },            {                \"name\": \"ARC-Authentication-Results\",                \"value\": \"i=1; mx.microsoft.com 1; spf=pass smtp.mailfrom=mastek.com; dmarc=pass action=none header.from=mastek.com; dkim=pass header.d=mastek.com; arc=none\"            },            {                \"name\": \"DKIM-Signature\",                \"value\": \"v=1; a=rsa-sha256; c=relaxed/relaxed; d=mastek.com; s=selector2; h=From:Date:Subject:Message-ID:Content-Type:MIME-Version:X-MS-Exchange-SenderADCheck; bh=j4NmNQnJpsyDil7EOOmYWCKQ+PTPtXEf+Qib3nn/+Fc=; b=HWyUJwHzaMUYEyzBKZPaxoo7Tcx55ogn1B0VuMqWbo9SfoGWcvNC2GmuxemomCotadWWsSVGCYwOnhboxnVDxOqWR9v+FtpjnLLpxkpCODv4aUI98PF8yvX7PEziBQS4YRMARaer5LgNrr5QIwIMMWhnIB06bOnI4E+g8H+PaGw=\"            },            {                \"name\": \"Received\",                \"value\": \"from KL1PR0601MB3718.apcprd06.prod.outlook.com (2603:1096:820:11::16) by KL1PR0601MB1974.apcprd06.prod.outlook.com (2603:1096:802:b::19) with Microsoft SMTP Server (version=TLS1_2, cipher=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384) id 15.20.2900.24; Fri, 17 Apr 2020 15:59:34 +0000\"            },            {                \"name\": \"Received\",                \"value\": \"from KL1PR0601MB3718.apcprd06.prod.outlook.com ([fe80::ac4c:5ddc:d876:fd63]) by KL1PR0601MB3718.apcprd06.prod.outlook.com ([fe80::ac4c:5ddc:d876:fd63%7]) with mapi id 15.20.2921.027; Fri, 17 Apr 2020 15:59:33 +0000\"            },            {                \"name\": \"From\",                \"value\": \"Sagar P Ghagare <Sagar.Ghagare@mastek.com>\"            },            {                \"name\": \"To\",                \"value\": \"\\\"stgeorges@iucdspilot.uk\\\" <stgeorges@iucdspilot.uk>\"            },            {                \"name\": \"Subject\",                \"value\": \"Test\"            },            {                \"name\": \"Thread-Topic\",                \"value\": \"Test\"            },            {                \"name\": \"Thread-Index\",                \"value\": \"AdYU0MlksQnjzGSvTF2BHFZYBeRajw==\"            },            {                \"name\": \"Date\",                \"value\": \"Fri, 17 Apr 2020 15:59:33 +0000\"            },            {                \"name\": \"Message-ID\",                \"value\": \"<KL1PR0601MB371883FFA41E140E977651C687D90@KL1PR0601MB3718.apcprd06.prod.outlook.com>\"            },            {                \"name\": \"Accept-Language\",                \"value\": \"en-US\"            },            {                \"name\": \"Content-Language\",                \"value\": \"en-US\"            },            {                \"name\": \"X-MS-Has-Attach\",                \"value\": \"yes\"            },            {                \"name\": \"X-MS-TNEF-Correlator\",                \"value\": \"\"            },            {                \"name\": \"authentication-results\",                \"value\": \"spf=none (sender IP is ) smtp.mailfrom=Sagar.Ghagare@mastek.com; \"            },            {                \"name\": \"x-originating-ip\",                \"value\": \"[88.98.242.249]\"            },            {                \"name\": \"x-ms-publictraffictype\",                \"value\": \"Email\"            },            {                \"name\": \"x-ms-office365-filtering-correlation-id\",                \"value\": \"8dfb0ca9-a2cb-416f-dfde-08d7e2e8526d\"            },            {                \"name\": \"x-ms-traffictypediagnostic\",                \"value\": \"KL1PR0601MB1974:\"            },            {                \"name\": \"x-microsoft-antispam-prvs\",                \"value\": \"<KL1PR0601MB1974175588BA1E4C1524095987D90@KL1PR0601MB1974.apcprd06.prod.outlook.com>\"            },            {                \"name\": \"x-ms-oob-tlc-oobclassifiers\",                \"value\": \"OLM:9508;\"            },            {                \"name\": \"x-forefront-prvs\",                \"value\": \"0376ECF4DD\"            },            {                \"name\": \"x-forefront-antispam-report\",                \"value\": \"CIP:255.255.255.255;CTRY:;LANG:en;SCL:1;SRV:;IPV:NLI;SFV:NSPM;H:KL1PR0601MB3718.apcprd06.prod.outlook.com;PTR:;CAT:NONE;SFTY:;SFS:(10009020)(4636009)(396003)(346002)(136003)(376002)(39850400004)(366004)(64756008)(3480700007)(7696005)(66946007)(66446008)(2906002)(6506007)(66476007)(73894004)(76116006)(99936003)(9686003)(71200400001)(66556008)(186003)(66616009)(86362001)(55016002)(7116003)(5660300002)(52536014)(478600001)(33656002)(26005)(4270600006)(316002)(6916009)(8676002)(81156014)(8936002);DIR:OUT;SFP:1101;\"            },            {                \"name\": \"received-spf\",                \"value\": \"None (protection.outlook.com: mastek.com does not designate permitted sender hosts)\"            },            {                \"name\": \"x-ms-exchange-senderadcheck\",                \"value\": \"1\"            },            {                \"name\": \"x-microsoft-antispam\",                \"value\": \"BCL:0;\"            },            {                \"name\": \"x-microsoft-antispam-message-info\",                \"value\": \"ZN1JXMKM+iALLwFtDy3PqgLWHoctWC0D/rd4ebpC0S6EngSaeQAIyJmf0ikALJ3AlLYD8Yv9WkcJKVuDiXQqQdMnBhC0MPHBQX//PkJNa34Prly9DLJBs8hrLrrpty+ALL14Cuf8yqVlLtZYfjbWPcf+ll4MNwPVhMu7NfXsJxICCcN7hv4hqcF29lj3JWTt8hLz1T8cibs/U5NXvpgtMOyKkbJyF0st5WSI4wvJXSxPy2QQlgVLfJafbMbGV/D62CY0kYsX25d6ccNlqXDAdvKjkJlYfIEvrH0P6cRh1p68Eu2jnenxqrWuTXgw9aXi\"            },            {                \"name\": \"x-ms-exchange-antispam-messagedata\",                \"value\": \"j6NOpkq2mlPqNSZC8BjA97yMOcuVIqf4mkTDrR608fnZnjlZr75W1soLMZx82TRgxHvEVCFWqf4qDOZaTkiEP/KWNtjQtLrOmblWmqeUvBpEtLjuKukDvnJvTMZRDgu52LH01GpzhJKrEbZ+IyoP0w==\"            },            {                \"name\": \"x-ms-exchange-transport-forked\",                \"value\": \"True\"            },            {                \"name\": \"Content-Type\",                \"value\": \"multipart/mixed; boundary=\\\"_004_KL1PR0601MB371883FFA41E140E977651C687D90KL1PR0601MB3718_\\\"\"            },            {                \"name\": \"MIME-Version\",                \"value\": \"1.0\"            },            {                \"name\": \"X-OriginatorOrg\",                \"value\": \"mastek.com\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-Network-Message-Id\",                \"value\": \"8dfb0ca9-a2cb-416f-dfde-08d7e2e8526d\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-originalarrivaltime\",                \"value\": \"17 Apr 2020 15:59:33.7398 (UTC)\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-fromentityheader\",                \"value\": \"Hosted\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-id\",                \"value\": \"add1c500-a6d7-4dbd-b890-7f8cb6f7d861\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-mailboxtype\",                \"value\": \"HOSTED\"            },            {                \"name\": \"X-MS-Exchange-CrossTenant-userprincipalname\",                \"value\": \"XQXcF60hBIl1MYfypmQWRBzEGqmyqj58BKCOaCaSraHsU7p6Ai64gbPT2QBBuYazRebP5CSwzUj/ULF2LEJOEw==\"            },            {                \"name\": \"X-MS-Exchange-Transport-CrossTenantHeadersStamped\",                \"value\": \"KL1PR0601MB1974\"            }        ],        \"commonHeaders\": {            \"returnPath\": \"Sagar.Ghagare@mastek.com\",            \"from\": [                \"Sagar P Ghagare <Sagar.Ghagare@mastek.com>\"            ],            \"date\": \"Fri, 17 Apr 2020 15:59:33 +0000\",            \"to\": [                \"\\\"stgeorges@iucdspilot.uk\\\" <stgeorges@iucdspilot.uk>\"            ],            \"messageId\": \"<KL1PR0601MB371883FFA41E140E977651C687D90@KL1PR0601MB3718.apcprd06.prod.outlook.com>\",            \"subject\": \"Test\"        }    },    \"receipt\": {        \"timestamp\": \"2020-04-17T15:59:39.639Z\",        \"processingTimeMillis\": 1114,        \"recipients\": [            \"stgeorges@iucdspilot.uk\"        ],        \"spamVerdict\": {            \"status\": \"PASS\"        },        \"virusVerdict\": {            \"status\": \"PASS\"        },        \"spfVerdict\": {            \"status\": \"PASS\"        },        \"dkimVerdict\": {            \"status\": \"PASS\"        },        \"dmarcVerdict\": {            \"status\": \"PASS\"        },        \"action\": {            \"type\": \"S3\",            \"topicArn\": \"arn:aws:sns:eu-west-1:410123189863:ems-receive-email-stgeorges\",            \"bucketName\": \"ems-receive-email-stgeorges\",            \"objectKeyPrefix\": \"\",            \"objectKey\": \"r6h454jgasdsdot7pgu2dmu5s404qiisn251k901\"        }    }}");
    SNSRecord snsRecord = new SNSRecord();
    snsRecord.setSns(sns);
    snsEvent.setRecords(Collections.singletonList(snsRecord));
    return snsEvent;
}
}

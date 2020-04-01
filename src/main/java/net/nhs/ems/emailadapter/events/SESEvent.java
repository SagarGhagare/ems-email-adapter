package net.nhs.ems.emailadapter.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;

/**
 * Represents an Amazon SES event.
 * @author https://github.com/gonfva
 * @see https://gist.github.com/gonfva/b249f76893165bf5a8d1
 */
public class SESEvent implements Serializable {

    /**
     * Represents an SES message
     *
     */
    public static class SES {
        private SESReceipt receipt;

        private SESMail mail;

        public SESReceipt getReceipt() {
            return receipt;
        }

        public void setReceipt(SESReceipt receipt) {
            this.receipt = receipt;
        }

        public SESMail getMail() {
            return mail;
        }

        public void setMail(SESMail mail) {
            this.mail = mail;
        }
    }


    public static class SESReceipt {
        private SESReceiptAction action;
        //Object that encapsulates information about the action that was executed.
        private SESDkimVerdict dkimVerdict;
        //Object that indicates whether the DomainKeys Identified Mail (DKIM) check passed.
        private String processingTimeMillis;
        //String that specifies the period, in milliseconds, from the time Amazon SES received the message to the time it triggered the action.
        private List<String> recipients;
        //A list of the recipient addresses for this delivery. This list might be a subset of the recipients to which the mail was addressed.
        private SESSpamVerdict spamVerdict;
        //Object that indicates whether the message is spam.
        private SESSpfVerdict spfVerdict;
        //Object that indicates whether the Sender Policy Framework (SPF) check passed.
        private String timestamp;
        //String that specifies when the action was triggered, in ISO8601 format.
        private SESVirusVerdict virusVerdict;
        //Object that indicates whether the message contains a virus.


        public SESReceiptAction getAction() {
            return action;
        }

        public void setAction(SESReceiptAction action) {
            this.action = action;
        }

        public SESDkimVerdict getDkimVerdict() {
            return dkimVerdict;
        }

        public void setDkimVerdict(SESDkimVerdict dkimVerdict) {
            this.dkimVerdict = dkimVerdict;
        }

        public String getProcessingTimeMillis() {
            return processingTimeMillis;
        }

        public void setProcessingTimeMillis(String processingTimeMillis) {
            this.processingTimeMillis = processingTimeMillis;
        }

        public List<String> getRecipients() {
            return recipients;
        }

        public void setRecipients(List<String> recipients) {
            this.recipients = recipients;
        }

        public SESSpamVerdict getSpamVerdict() {
            return spamVerdict;
        }

        public void setSpamVerdict(SESSpamVerdict spamVerdict) {
            this.spamVerdict = spamVerdict;
        }

        public SESSpfVerdict getSpfVerdict() {
            return spfVerdict;
        }

        public void setSpfVerdict(SESSpfVerdict spfVerdict) {
            this.spfVerdict = spfVerdict;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public SESVirusVerdict getVirusVerdict() {
            return virusVerdict;
        }

        public void setVirusVerdict(SESVirusVerdict virusVerdict) {
            this.virusVerdict = virusVerdict;
        }


    }

    public static class SESReceiptAction {
        private String type;
        //String that indicates the type of action that was executed. Possible values are S3Action, SNSAction, BounceAction, LambdaAction, StopAction, and WorkMailAction.
        private String topicArn;
        //String that contains the Amazon Resource Name (ARN) of the Amazon SNS topic to which the notification was published.
        private String bucketName;
        //String that contains the name of the Amazon S3 bucket to which the message was published. Present only for the S3 action type.
        private String objectKey;
        //String that contains a name that uniquely identifies the email in the Amazon S3 bucket. This is the same as the messageId in the mail object. Present only for the S3 action type.
        private String smtpReplyCode;
        //String that contains the SMTP reply code, as defined by RFC 5321. Present only for the bounce action type.
        private String statusCode;
        //String that contains the SMTP enhanced status code, as defined by RFC 3463. Present only for the bounce action type.
        private String message;
        //String that contains the human-readable text to include in the bounce message. Present only for the bounce action type.
        private String sender;
        //String that contains the email address of the sender of the email that bounced. This is the address from which the bounce message was sent. Present only for the bounce action type.
        private String functionArn;
        //String that contains the ARN of the Lambda function that was triggered. Present only for the Lambda action type.
        private String invocationType;
        //String that contains the invocation type of the Lambda function. Possible values are RequestResponse and Event. Present only for the Lambda action type.
        private String organizationArn;
        //String that contains the ARN of the Amazon WorkMail organization. Present only for the WorkMail action type.


        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getTopicArn() {
            return topicArn;
        }

        public void setTopicArn(String topicArn) {
            this.topicArn = topicArn;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }

        public String getSmtpReplyCode() {
            return smtpReplyCode;
        }

        public void setSmtpReplyCode(String smtpReplyCode) {
            this.smtpReplyCode = smtpReplyCode;
        }

        public String getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getSender() {
            return sender;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public String getFunctionArn() {
            return functionArn;
        }

        public void setFunctionArn(String functionArn) {
            this.functionArn = functionArn;
        }

        public String getInvocationType() {
            return invocationType;
        }

        public void setInvocationType(String invocationType) {
            this.invocationType = invocationType;
        }

        public String getOrganizationArn() {
            return organizationArn;
        }

        public void setOrganizationArn(String organizationArn) {
            this.organizationArn = organizationArn;
        }
    }

    public static class SESSpamVerdict {
        private String status;

        /**
         * Get the Spam status
         * Possible values are PASS, FAIL, GRAY, or PROCESSING_FAILED.
         * A value of GRAY indicates that the result is indeterminate.
         * @return String that contains the result of spam scanning.
         */
        public String getStatus() {
            return status;
        }

        /**
         * Sets the Spam status
         * @param status
         */
        public void setStatus(String status) {
            this.status = status;
        }


    }

    public static class SESSpfVerdict {
        private String status;
        /**
         * Gets the SPF veredict.
         * Possible values are PASS, FAIL, GRAY, or PROCESSING_FAILED.
         * A value of GRAY indicates that the result is indeterminate.
         * @return string containing SPF veredict
         */
        public String getStatus() {
            return status;
        }

        /**
         * Sets the SPF status
         * @param status
         */
        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class SESVirusVerdict {
        private String status;
        /**
         * Gets the result of virus scanning.
         * Possible values are PASS, FAIL, GRAY, or PROCESSING_FAILED.
         * A value of GRAY indicates that the result is indeterminate.
         * @return string containing Virus veredict
         */
        public String getStatus() {
            return status;
        }

        /**
         * Sets the Virus status
         * @param status
         */
        public void setStatus(String status) {
            this.status = status;
        }

    }

    public static class SESDkimVerdict{
        private String status;
        /**
         * Gets the DKIM verdict.
         * Possible values are PASS, FAIL, GRAY, or PROCESSING_FAILED.
         * A value of GRAY indicates that the result is indeterminate.
         */
        /**
         * Gets the SPF veredict.
         * Possible values are PASS, FAIL, GRAY, or PROCESSING_FAILED.
         * A value of GRAY indicates that the result is indeterminate.
         * @return string containing SPF veredict
         */
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class SESMail {
        private List<String> destination;
        //A list of email addresses that are recipients of the email.
        private String messageId;
        //String that contains the unique ID assigned to the email by Amazon SES. If the email was delivered to Amazon S3, the message ID is also the Amazon S3 object key that was used to write the message to your Amazon S3 bucket.
        private String source;
        //String that contains the email address from which the email was sent (the envelope MAIL FROM address).
        private String timestamp;
        //String that contains the time at which the email was received, in ISO8601 format.
        private List<MessageHeader> headers;
        //A list of Amazon SES headers and your custom headers. Each header in the list has a name field and a value field.
        private CommonHeaders commonHeaders;
        //A list of headers common to all emails. Each header in the list has a name field and a value field.
        private String headersTruncated;
        //String that specifies whether the headers were truncated in the notification, which will happen if the headers are larger than 10 KB. Possible values are true and false.


        public List<String> getDestination() {
            return destination;
        }

        public void setDestination(List<String> destination) {
            this.destination = destination;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public List<MessageHeader> getHeaders() {
            return headers;
        }

        public void setHeaders(List<MessageHeader> headers) {
            this.headers = headers;
        }

        public CommonHeaders getCommonHeaders() {
            return commonHeaders;
        }

        public void setCommonHeaders(CommonHeaders commonHeaders) {
            this.commonHeaders = commonHeaders;
        }

        public String getHeadersTruncated() {
            return headersTruncated;
        }

        public void setHeadersTruncated(String headersTruncated) {
            this.headersTruncated = headersTruncated;
        }

    }
    public static class CommonHeaders {
        private String returnPath;

        private List<String> from;

        private String date;

        private List<String> to;

        private String messageId;

        private String subject;

        public String getReturnPath() {
            return returnPath;
        }

        public void setReturnPath(String returnPath) {
            this.returnPath = returnPath;
        }

        public List<String> getFrom() {
            return from;
        }

        public void setFrom(List<String> from) {
            this.from = from;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public List<String> getTo() {
            return to;
        }

        public void setTo(List<String> to) {
            this.to = to;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }
    }

    /**
     * Represents an SES message record. SES message Records are used to send
     * SES messages to Lambda Functions.
     *
     */
    public static class SESRecord {
        private SES ses;

        private String eventVersion;

        private String eventSource;

        /**
         *  Gets the SES message         *
         */
        public SES getSES() {
            return ses;
        }

        /**
         * Sets the SES message
         * @param ses An SES object representing the SES message
         */
        public void setSes(SES ses) {
            this.ses = ses;
        }

        /**
         * Gets the event version         *
         */
        public String getEventVersion() {
            return eventVersion;
        }

        /**
         * Sets the event version
         * @param eventVersion A string containing the event version
         */
        public void setEventVersion(String eventVersion) {
            this.eventVersion = eventVersion;
        }

        /**
         * Gets the event source
         *
         */
        public String getEventSource() {
            return eventSource;
        }

        /**
         * Sets the event source
         * @param eventSource A string containing the event source
         */
        public void setEventSource(String eventSource) {
            this.eventSource = eventSource;
        }

    }

    /**
     * Represents an SES message attribute
     *
     */
    public static class MessageHeader {
        private String name;
        private String value;

        /**
         * Gets the attribute type
         *
         */
        public String getName() {
            return name;
        }

        /**
         * Gets the attribute value
         *
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the attribute type
         * @param name A string representing the attribute type
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Sets the attribute value
         * @param value A string containing the attribute value
         */
        public void setValue(String value) {
            this.value = value;
        }
    }

    private List<SESRecord> records;

    /**
     *  Gets the list of SES Records
     *
     */
    @JsonProperty("Records")
    public List<SESRecord> getRecords() {
        return records;
    }

    /**
     * Sets a list of SES Records
     * @param records A list of SES record objects
     */
    public void setRecords(@JsonProperty("Records") List<SESRecord> records) {
        this.records = records;
    }

}

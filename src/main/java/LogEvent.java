import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TimeToLiveSpecification;
import com.amazonaws.services.dynamodbv2.model.UpdateTimeToLiveRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
public class LogEvent implements RequestHandler<SNSEvent, Object> {

    static DynamoDB dynamoDB;


    public Object handleRequest(SNSEvent request, Context context) {

        String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        context.getLogger().log("Invocation started: " + timeStamp);

        context.getLogger().log("1: " + (request == null));

        context.getLogger().log("2: " + (request.getRecords().size()));

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());

        timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            context.getLogger().log("trying to connect to dynamodb");
            init();
            long unixTime = Instant.now().getEpochSecond()+20*60;
            Table table = dynamoDB.getTable("csye6225");

            Item item = table.getItem("id", request.getRecords().get(0).getSNS().getMessage());

            if(item==null) {
                Item itemPut = new Item()
                        .withPrimaryKey("id", request.getRecords().get(0).getSNS().getMessage())
                        .withString("token", context.getAwsRequestId())
                        .withNumber("passwordTokenExpiry", unixTime);

                table.putItem(itemPut);

                try {
                    String FROM = "donotreply@csye6225-fall2018-janhaviu.me";
                    String TO = request.getRecords().get(0).getSNS().getMessage();
                    String token = request.getRecords().get(0).getSNS().getMessageId();
                    AmazonSimpleEmailService client =
                            AmazonSimpleEmailServiceClientBuilder.standard()
                                    .withRegion(Regions.US_EAST_1).build();
                    SendEmailRequest req = new SendEmailRequest()
                            .withDestination(
                                    new Destination()
                                            .withToAddresses(TO))
                            .withMessage(
                                    new Message()
                                            .withBody(
                                                    new Body()
                                                            .withHtml(
                                                                    new Content()
                                                                            .withCharset(
                                                                                    "UTF-8")
                                                                            .withData(
                                                                                    "Please click on the below link to reset the password<br/>"+
                                                                                            "<p><a href='#'>https://csye6225-fall2018-janhaviu.me/reset?email="+TO+"&token="+token+"</a></p>"))
                                            )
                                            .withSubject(
                                                    new Content().withCharset("UTF-8")
                                                            .withData("Password Reset Link")))
                            .withSource(FROM);
                    SendEmailResult response = client.sendEmail(req);
                    context.getLogger().log ("Email sent!");
                } catch (Exception ex) {
                    context.getLogger().log ("The email was not sent. Error message: "
                            + ex.getMessage());
                }


            }
        }

        catch(AmazonServiceException ase){
            context.getLogger().log("Could not complete operation");
            context.getLogger().log("Error Message:  " + ase.getMessage());
            context.getLogger().log("HTTP Status:    " + ase.getStatusCode());
            context.getLogger().log("AWS Error Code: " + ase.getErrorCode());
            context.getLogger().log("Error Type:     " + ase.getErrorType());
            context.getLogger().log("Request ID:     " + ase.getRequestId());
        }
        catch (AmazonClientException ace) {
            context.getLogger().log("Internal error occured communicating with DynamoDB");
            context.getLogger().log("Error Message:  " + ace.getMessage());
        }
        catch(Exception e){
            context.getLogger().log(e.getMessage());
        }

        context.getLogger().log("Invocation completed: " + timeStamp);

        return null;
    }

    private static void init() throws Exception {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        dynamoDB = new DynamoDB(client);
    }


}
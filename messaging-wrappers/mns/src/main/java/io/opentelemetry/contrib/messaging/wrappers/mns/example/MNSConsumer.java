package io.opentelemetry.contrib.messaging.wrappers.mns.example;

import com.aliyun.mns.client.CloudAccount;
import com.aliyun.mns.client.CloudQueue;
import com.aliyun.mns.client.MNSClient;
import com.aliyun.mns.common.ClientException;
import com.aliyun.mns.common.ServiceException;
import com.aliyun.mns.model.Message;
import com.aliyun.mns.model.MessageSystemPropertyValue;
import io.opentelemetry.contrib.messaging.wrappers.MessagingProcessWrapper;
import io.opentelemetry.contrib.messaging.wrappers.mns.MNSHelper;
import io.opentelemetry.contrib.messaging.wrappers.mns.semconv.MNSProcessRequest;

import static com.aliyun.mns.model.MessageSystemPropertyName.TRACE_PARENT;

public class MNSConsumer {

  public static void main(String[] args) {
    // 1. create wrapper by default
    MessagingProcessWrapper<MNSProcessRequest> wrapper = MNSHelper.processWrapperBuilder().build();
    CloudAccount account = new CloudAccount("my-ak", "my-sk", "endpoint");
    MNSClient client = account.getMNSClient();

    final String queueName = "test-queue";
    try {
      CloudQueue queue = client.getQueueRef(queueName);
      while (true) {
        Message popMsg = queue.popMessage(5);
        if (popMsg != null) {
          // 2. wrap your consume block
          String result = wrapper.doProcess(MNSProcessRequest.of(popMsg, queueName), () -> {
            System.out.println("message handle: " + popMsg.getReceiptHandle());
            System.out.println("message body: " + popMsg.getMessageBodyAsString());
            System.out.println("message id: " + popMsg.getMessageId());
            System.out.println("message dequeue count:" + popMsg.getDequeueCount());
            MessageSystemPropertyValue systemProperty = popMsg.getSystemProperty(TRACE_PARENT);
            if (systemProperty != null) {
              System.out.println("message trace parent: " + systemProperty.getStringValueByType());
            } else {
              System.out.println("empty system property");
            }
            //<<to add your special logic.>>

            queue.deleteMessage(popMsg.getReceiptHandle());
            System.out.println("delete message successfully\n");
            return "success";
          });
        }
      }
    } catch (ClientException ce) {
      System.out.println("Something wrong with the network connection between client and MNS service."
          + "Please check your network and DNS availablity.");
      ce.printStackTrace();
    } catch (ServiceException se) {
      if (se.getErrorCode().equals("QueueNotExist")) {
        System.out.println("Queue is not exist.Please create queue before use");
      } else if (se.getErrorCode().equals("TimeExpired")) {
        System.out.println("The request is time expired. Please check your local machine timeclock");
      }
      /*
      you can get more MNS service error code in following link.
      https://help.aliyun.com/document_detail/mns/api_reference/error_code/error_code.html?spm=5176.docmns/api_reference/error_code/error_response
      */
      se.printStackTrace();
    } catch (Exception e) {
      System.out.println("Unknown exception happened!");
      e.printStackTrace();
    }

    client.close();
  }
}

package cn.geekcity.xiot.xcp.vertx.impl.ws.client.example;

import cn.geekcity.xiot.crypto.ed25519.Ed25519KeyPair;
import cn.geekcity.xiot.spec.definition.urn.DeviceType;
import cn.geekcity.xiot.spec.operation.PropertyOperation;
import cn.geekcity.xiot.spec.status.Status;
import cn.geekcity.xiot.xcp.common.XcpFrameCodecType;
import cn.geekcity.xiot.xcp.stanza.iq.device.control.GetProperties;
import cn.geekcity.xiot.xcp.stanza.iq.device.control.InvokeActions;
import cn.geekcity.xiot.xcp.stanza.iq.device.control.SetProperties;
import cn.geekcity.xiot.xcp.stanza.iq.device.key.GetAccessKey;
import cn.geekcity.xiot.xcp.vertx.XcpDeviceClient;
import cn.geekcity.xiot.xcp.vertx.XcpDeviceClientCipher;
import cn.geekcity.xiot.xcp.vertx.XcpLTSKGetter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpClientOptions;
import org.bouncycastle.util.encoders.Base64;

public class TwoKeySwitch extends AbstractVerticle {

	private static final String SERVER_LTPK = "/8meBcfecxNl7pMIO0Zxbhx70A4DSGio7C2H7VzZLB8=";
	private static final String SERIAL_NUMBER = "abc";
	private static final String DEVICE_TYPE = "urn:homekit-spec:device:switch:00000008:generic:s2:1";
	private static final int PRODUCT_ID = 3;
	private static final String SERVER_IP = "dev-iot-ap.knowin.com";
	private static final int SERVER_PORT = 80;

	private static class DeviceLTSKGetter implements XcpLTSKGetter {

		private static final String SEED = "MC44OTA4NTIyMDExMTk1NjUy";
		private static final String LTSK = "cPyALYzzdcuUdFPaJgO/PBXzX3drpHdEff73AhOxpUQ=";
		private static final String LTPK = "1WT5k/pV+AHddLG4jLlKb0fGQl/5np+7X+6Wm1IvDPg=";

		@Override
		public Ed25519KeyPair getKeypair(String deviceId) {
			return new Ed25519KeyPair(Base64.decode(SEED));
		}

		@Override
		public Ed25519KeyPair getKeypair(DeviceType deviceType) {
			return new Ed25519KeyPair(Base64.decode(SEED));
		}
	}

	private XcpDeviceClient xcpDeviceClient;

	@Override
	public void start() throws Exception {
		XcpDeviceClientCipher cipher = XcpDeviceClientCipher.create(new DeviceType(DEVICE_TYPE),
				new DeviceLTSKGetter(),
				Base64.decode(SERVER_LTPK));

		HttpClientOptions options = new HttpClientOptions();
		options.setKeepAlive(true);
		options.setKeepAliveTimeout(30);

		xcpDeviceClient = XcpDeviceClient.createWs(vertx,
				options,
				PRODUCT_ID,
				SERIAL_NUMBER,
				DEVICE_TYPE,
				cipher,
				XcpFrameCodecType.NOT_CRYPT);

		xcpDeviceClient.connect(SERVER_IP, SERVER_PORT, ar -> {
			if (ar.succeeded()) {
				System.out.println("Connected to a server");

				xcpDeviceClient.closeHandler(x -> System.out.println("closed"));

				xcpDeviceClient.exceptionHandler(x -> System.out.println("exception: " + x.toString()));

				xcpDeviceClient.addQueryHandler(GetProperties.METHOD, query -> onGetProperties((GetProperties.Query) query));

				xcpDeviceClient.addQueryHandler(SetProperties.METHOD, query -> {
					SetProperties.Query q = (SetProperties.Query) query;
					System.out.println("XcpSetProperties: " + q.id());
				});

				xcpDeviceClient.addQueryHandler(InvokeActions.METHOD, query -> {
					InvokeActions.Query q = (InvokeActions.Query) query;
					System.out.println("XcpInvokeAction: " + q.id());
				});

				showAccessKey();

			} else {
				ar.cause().printStackTrace();
				System.out.println("Failed to connect to a server");
			}
		});
	}

	@Override
	public void stop() throws Exception {
		xcpDeviceClient.disconnect();
	}

	private void showAccessKey() {
		GetAccessKey.Query query = new GetAccessKey.Query(xcpDeviceClient.nextStanzaId());
		xcpDeviceClient.send(query, ar -> {
			if (ar.succeeded()) {
				GetAccessKey.Result result = (GetAccessKey.Result) ar.result();
				System.out.println("did: " + xcpDeviceClient.did());
				System.out.println("accessKey: " + result.key());
			} else {
				ar.cause().printStackTrace();
			}
		});
	}

	private void onGetProperties(GetProperties.Query query) {
		for (PropertyOperation o : query.properties()) {
			switch (o.siid()) {
				case 1:
					onGetProperties(o);
					break;

				case 10:
					onGetLeftButton(o);
					break;

				case 13:
					onGetRightButton(o);
					break;

				default:
					o.status(Status.SERVICE_NOT_FOUND);
					o.description("service not found");
					break;
			}
		}
	}

	private void onGetProperties(PropertyOperation o) {
		switch (o.iid()) {
			case 3:
				o.status(Status.PROPERTY_CANNOT_NOTIFY);
				o.status(Status.PROPERTY_CANNOT_READ);
				o.value(false);
				break;

			case 4:
				o.status(Status.PROPERTY_CANNOT_NOTIFY);
				o.status(Status.PROPERTY_CANNOT_WRITE);
				o.value("aaa");
				break;

			case 5:
				o.status(Status.PROPERTY_CANNOT_NOTIFY);
				o.status(Status.PROPERTY_CANNOT_WRITE);
				o.value("xxx");
				break;

			case 6:
				o.status(Status.PROPERTY_CANNOT_NOTIFY);
				o.status(Status.PROPERTY_CANNOT_WRITE);
				o.value("111");
				break;

			case 7:
				o.status(Status.PROPERTY_CANNOT_NOTIFY);
				o.status(Status.PROPERTY_CANNOT_WRITE);
				o.value("646");
				break;

			default:
				o.status(Status.PROPERTY_NOT_FOUND);
				break;
		}
	}

	private void onGetLeftButton(PropertyOperation o) {
		switch (o.iid()) {
			case 11:
				o.value(true);
				break;


			default:
				o.status(Status.PROPERTY_NOT_FOUND);
				o.description("property not found");
				break;
		}
	}

	private void onGetRightButton(PropertyOperation o) {
		switch (o.iid()) {
			case 14:
				o.value(true);
				break;

			default:
				o.status(Status.PROPERTY_NOT_FOUND);
				o.description("property not found");
				break;
		}
	}
}

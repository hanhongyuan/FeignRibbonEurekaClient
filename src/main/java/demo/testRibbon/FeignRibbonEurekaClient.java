package demo.testRibbon;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import demo.testRibbon.model.Catalog;
import feign.Feign;
import feign.Logger;
import feign.RequestLine;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.ribbon.RibbonClient;

public class FeignRibbonEurekaClient {
	interface CatalogService {

		@RequestLine("GET /v1/catalog")
		Catalog getCatalog();
	}

	private static ApplicationInfoManager applicationInfoManager;
	private static EurekaClient eurekaClient;

	private static synchronized ApplicationInfoManager initializeApplicationInfoManager(
			EurekaInstanceConfig instanceConfig) {
		if (applicationInfoManager == null) {
			InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
			applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
		}

		return applicationInfoManager;
	}

	private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager,
			EurekaClientConfig clientConfig) {
		if (eurekaClient == null) {
			eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
		}

		return eurekaClient;
	}

	private static synchronized void initializeRibbonClient() throws Exception {
		ConfigurationManager.loadPropertiesFromResources("sample-client.properties"); // 1
	}

	private static synchronized void init() throws Exception {
		ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(
				new MyDataCenterInstanceConfig());
		initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());

		initializeRibbonClient();
	}

	public static void main(String[] args) throws Exception {
		init();

		Decoder decoder = new JacksonDecoder();
		Encoder encoder = new JacksonEncoder();
		Logger logger = new Logger.JavaLogger();

		CatalogService api = Feign.builder().encoder(encoder).decoder(decoder).logger(logger)
				.logLevel(Logger.Level.HEADERS).client(RibbonClient.create())
				.target(CatalogService.class, "http://sample-client");

		Catalog c = api.getCatalog();

		System.out.println(c);

	}

}

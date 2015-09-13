package itti.com.pl.transcoder.service;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import itti.com.pl.transcoder.dto.Bitrate;
import itti.com.pl.transcoder.dto.Configuration;
import itti.com.pl.transcoder.dto.Size;
import itti.com.pl.transcoder.dto.State;
import itti.com.pl.transcoder.helper.BitrateMapper;
import itti.com.pl.transcoder.helper.JSONApi;
import itti.com.pl.transcoder.helper.ProcessHelper;
import itti.com.pl.transcoder.helper.SizeMapper;
import itti.com.pl.transcoder.helper.SocketApi;
import itti.com.pl.transcoder.service.Event.EventBuilder;

@Service
public class TranscoderFacade {

	private static final Log LOG = LogFactory.getLog(TranscoderFacade.class);

	private static final String ACTION = "configure";

	@Autowired
	private Environment environment;

	private Configuration configuration = null;

	private SocketApi socketApi;

	private ProcessHelper processHelper;

	@PostConstruct
	private void postConstruct(){
		setProcessHelper(getDefaultProcessHelper());
		setSocketAPI(getDefaultSocketAPI());
		setConfiguration(getDefaultConfiguration());
		LOG.info("postConstruct");
	}

	private void setProcessHelper(ProcessHelper processHelper) {
		this.processHelper = processHelper;
	}

	private SocketApi getDefaultSocketAPI() {
		SocketApi socketApi = new SocketApi(environment.getProperty("transcoder_admin_ip", String.class),
				environment.getProperty("transcoder_admin_port", Integer.class));
		LOG.debug(socketApi);
		return socketApi;
	}

	private Configuration getDefaultConfiguration() {
		Configuration configuration = new Configuration();
		configuration.setFps(environment.getProperty("fps", Integer.class));
		configuration.setBitrate(environment.getProperty("bitrate", Bitrate.class));
		configuration.setSize(environment.getProperty("size", Size.class));

		LOG.debug(configuration);
		return configuration;
	}

	private ProcessHelper getDefaultProcessHelper() {
		ProcessHelper processHelper = new ProcessHelper(
				environment.getProperty("sh_cmd", String.class),
				environment.getProperty("ps_cmd", String.class),
				environment.getProperty("kill_cmd", String.class));
		LOG.debug(processHelper);
		return processHelper;
	}

	void setSocketAPI(SocketApi socketApi) {
		this.socketApi = socketApi;
	}

	public void start() {

		if (!isStarted()) {
			processHelper.startProcess(environment.getProperty("start_cmd"));
		}
		updateConfiguration();
	}

	private boolean isStarted() {
		return processHelper.isProcessRunning(environment.getProperty("process_name"));
	}

	public void stop() {

		processHelper.destroyProcess(environment.getProperty("process_name"));
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
		if (isStarted()) {
			updateConfiguration();
		}
	}

	private void updateConfiguration() {
		setFps(configuration.getFps());
		setBitrate(configuration.getBitrate());
		setSize(configuration.getSize());
	}

	private void setFps(int fps) {
		socketApi.writeToSocket(JSONApi
				.objectToJson(new EventBuilder().withAction(ACTION).withParam("fps", fps).withFilterId(1002).buildOneItemList()));
		socketApi.writeToSocket(JSONApi
				.objectToJson(new EventBuilder().withAction(ACTION).withParam("fps", fps).withFilterId(1001).buildOneItemList()));
		socketApi.writeToSocket(JSONApi
				.objectToJson(new EventBuilder().withAction(ACTION).withParam("fps", fps).withFilterId(1000).buildOneItemList()));
	}

	private void setBitrate(Bitrate bitrate) {
		socketApi.writeToSocket(JSONApi.objectToJson(new EventBuilder().withAction(ACTION)
				.withParam("bitrate", BitrateMapper.getBitrate(bitrate) / 4).withFilterId(1002).buildOneItemList()));
		socketApi.writeToSocket(JSONApi.objectToJson(new EventBuilder().withAction(ACTION)
				.withParam("bitrate", BitrateMapper.getBitrate(bitrate) / 2).withFilterId(1001).buildOneItemList()));
		socketApi.writeToSocket(JSONApi.objectToJson(new EventBuilder().withAction(ACTION)
				.withParam("bitrate", BitrateMapper.getBitrate(bitrate)).withFilterId(1000).buildOneItemList()));
	}

	private void setSize(Size size) {
		socketApi.writeToSocket(JSONApi
				.objectToJson(new EventBuilder().withAction(ACTION).withParam("width", SizeMapper.getWidth(size) / 2)
						.withParam("height", SizeMapper.getHeight(size) / 2).withFilterId(2002).buildOneItemList()));
		socketApi.writeToSocket(
				JSONApi.objectToJson(new EventBuilder().withAction(ACTION).withParam("width", SizeMapper.getWidth(size))
						.withParam("height", SizeMapper.getHeight(size)).withFilterId(2001).buildOneItemList()));
		socketApi.writeToSocket(
				JSONApi.objectToJson(new EventBuilder().withAction(ACTION).withParam("width", SizeMapper.getWidth(size))
						.withParam("height", SizeMapper.getHeight(size)).withFilterId(2000).buildOneItemList()));
	}

	public Configuration getConfiguration() {
		Configuration cfg = new Configuration();
		cfg.setBitrate(configuration.getBitrate());
		cfg.setFps(configuration.getFps());
		cfg.setSize(configuration.getSize());
		return cfg;
	}

	public State getState() {
		return processHelper.isProcessRunning(environment.getProperty("process_name")) ? State.RUNNING : State.STOPPED;
	}

}

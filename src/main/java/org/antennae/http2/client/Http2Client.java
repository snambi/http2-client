package org.antennae.http2.client;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.http.MetaData;
import org.eclipse.jetty.http2.api.Session;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.api.Stream.Listener;
import org.eclipse.jetty.http2.api.server.ServerSessionListener;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.FuturePromise;
import org.eclipse.jetty.util.Jetty;
import org.eclipse.jetty.util.Promise;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class Http2Client {

	public static void main(String[] args) {

		String host = "localhost";
		int port = 8443;
		String app = "/h2";

		HTTP2Client client = new HTTP2Client();
		SslContextFactory sslContextFactory = new SslContextFactory(true);
		client.addBean(sslContextFactory);

		try {

			client.start();

			FuturePromise<Session> sessionPromise = new FuturePromise<Session>();
			client.connect(sslContextFactory, new InetSocketAddress(host, port), 
								new ServerSessionListener.Adapter(), sessionPromise);
			
			Session session = sessionPromise.get(5, TimeUnit.SECONDS);
			
	        HttpFields requestFields = new HttpFields();
	        requestFields.put("User-Agent", client.getClass().getName() + "/" + Jetty.VERSION);
	        MetaData.Request metaData = new MetaData.Request("GET", new HttpURI("https://" + host + ":" + port + app), HttpVersion.HTTP_2, requestFields);
	        HeadersFrame headersFrame = new HeadersFrame(metaData, null, true);
	       
	        final Phaser phaser = new Phaser(2);

	        Promise<Stream> promise = new Promise<Stream>(){
				public void succeeded(Stream result) {
					System.out.println("stream succeeded: " + result.getId() + ": " + result.toString());
				}

				public void failed(Throwable x) {
					System.out.println("stream failed: " + x.getStackTrace());
				}
	        };
	        
	        
	        session.newStream(headersFrame, promise, new Stream.Listener() {
				
				public void onTimeout(Stream stream, Throwable x) {
				}
				public void onReset(Stream stream, ResetFrame frame) {
				}
				public Listener onPush(Stream stream, PushPromiseFrame frame) {
					System.out.println("PUSH: "+ stream.getId() +", "+ frame.getMetaData() );
					return null;
				}
				public void onHeaders(Stream stream, HeadersFrame frame) {
					System.out.println("HEADERS: "+ stream.getId() + ", "+ frame.getMetaData().getFields());
				}
				public void onData(Stream stream, DataFrame frame, Callback callback) {
					System.out.println("DATA: "+ new String(frame.getData().array()) + ", callback: "+ callback.getClass() );
				}
			});
	        
	        phaser.awaitAdvanceInterruptibly(phaser.arrive(), 5, TimeUnit.SECONDS);
	        
	        client.stop();

		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

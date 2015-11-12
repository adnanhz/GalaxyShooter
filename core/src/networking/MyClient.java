package networking;

import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.galaxyshooter.game.Assets;
import com.galaxyshooter.game.Assets.GameSprite;

import components.BodyComponent;
import components.ControllableComponent;
import components.CoupledComponent;
import components.DamageSpriteComponent;
import components.DispatchableComponent;
import components.LaunchableComponent;
import components.LightComponent;
import components.OutOfBoundsComponent;
import components.ParticleComponent;
import components.PositionComponent;
import components.RelativeSpeedComponent;
import components.RenderableComponent;
import components.SizeComponent;
import components.SpeedComponent;
import components.SpriteComponent;

public class MyClient {
	private Client client;
	private Engine engine;
	private Packet packet;

	public MyClient(Engine engine) {
		this.engine = engine;
		client = new Client();
		packet = new Packet();
		Kryo kryo = client.getKryo();
		Kryos.registerAll(kryo);
		client.start();
		try {
			client.connect(5000, "localhost", 5000, 6000);
			clientListener();
		} catch (IOException e) {
			System.out.println("Could not connect to server");
			e.printStackTrace();
		}
	}

	private void clientListener() {
		client.addListener(new Listener() {

			@Override
			public void connected(Connection connection) {
			}

			@Override
			public void received(Connection connection, Object object) {
				if (object instanceof Packet) {
					Entity entity = new Entity();
					Array<Component> components = translatePacket((Packet) object);
					for(Component c: components)
						entity.add(c);
					engine.addEntity(entity);
					System.out.println("Added entity with the following components: "+components);
				}
			}

		});
	}

	public void sendPacket(ImmutableArray<Component> immutableArray) {
		packet.stuff = new HashMap<String, Object>();
		for (int i = 0; i < immutableArray.size(); i++) {
			SimpleEntry<String, Object> entry = translateComponent(immutableArray.get(i));
			if(entry != null)
				packet.stuff.put(entry.getKey(), entry.getValue());
		}
		
		client.sendUDP(packet);
	}

	private SimpleEntry<String, Object> translateComponent(Component component) {
		if (component instanceof SpriteComponent) {
			SpriteComponent sprite = (SpriteComponent) component;
			Object[] spriteInfo = new Object[2];
			int spriteID = Assets.getSpriteID(sprite.sprite);
			boolean afterLight = sprite.afterLight;
			spriteInfo[0] = spriteID;
			spriteInfo[1] = afterLight;
			return new SimpleEntry<String, Object>("Sprite", spriteInfo);

		}

		else if (component instanceof SpeedComponent) {
			SpeedComponent speed = (SpeedComponent) component;
			float[] speeds = { speed.x, speed.y };
			return new SimpleEntry<String, Object>("Speed", speeds);
		}

		else if (component instanceof SizeComponent) {
			SizeComponent size = (SizeComponent) component;
			float[] sizes = { size.width, size.height };
			return new SimpleEntry<String, Object>("Size", sizes);
		}

		else if (component instanceof RenderableComponent) {
			return new SimpleEntry<String, Object>("Renderable", true);
		} else if (component instanceof RelativeSpeedComponent) {
			return null; // TODO
		}

		else if (component instanceof PositionComponent) {
			PositionComponent position = (PositionComponent) component;
			int overridenByBody=0;
			if(position.overridenByBody)
				overridenByBody = 1;
			float[] positions = { position.x, position.y, overridenByBody};
			return new SimpleEntry<String, Object>("Position", positions);
		}

		else if (component instanceof ParticleComponent){
			ParticleComponent particle = (ParticleComponent) component;
			int particleID = particle.gameParticle.ordinal();
			
			return new SimpleEntry<String, Object>("Particle", particleID);
			
		}
			

		else if (component instanceof OutOfBoundsComponent) {
			OutOfBoundsComponent outOfBounds = (OutOfBoundsComponent) component;
			return new SimpleEntry<String, Object>("OutOfBounds",
					outOfBounds.action.ordinal());
		}

		else if (component instanceof LightComponent) {
			LightComponent light = (LightComponent) component;
			Object[] lightStuff = { light.color, light.distance, light.rays,
					light.x, light.y };
			return new SimpleEntry<String, Object>("Light", lightStuff);
		}

		else if (component instanceof LaunchableComponent) {
			return null;
		}

		else if (component instanceof DispatchableComponent)
			return null;

		else if (component instanceof DamageSpriteComponent) {
			DamageSpriteComponent damageSprite = (DamageSpriteComponent) component;
			int spriteID = Assets.getSpriteID(damageSprite.damageSprite);
			return new SimpleEntry<String, Object>("DamageSprite", spriteID);
		}

		else if (component instanceof CoupledComponent) {
			return null;
		}

		else if (component instanceof ControllableComponent) {
			return null;
		}

		else if (component instanceof BodyComponent) {
			BodyComponent body = (BodyComponent) component;
			float[][] vertices = null;
			if (body.vertices != null)
				vertices = new float[body.vertices.length][2];
			else
				return new SimpleEntry<String, Object>("Body", null);

			for (int i = 0; i < body.vertices.length; i++) {
				vertices[i][0] = body.vertices[i].x;
				vertices[i][1] = body.vertices[i].y;
			}

			return new SimpleEntry<String, Object>("Body", vertices);
		}

		return null;
	}
	
	private Array<Component> translatePacket(Packet packet){
		HashMap<String, Object> map = packet.stuff;
		Array<Component> components = new Array<Component>();
		
		for(String key: map.keySet()){
			if(key.equals("Sprite")){
				Object[] info = (Object[]) map.get(key);
				SpriteComponent sprite = new SpriteComponent();
				
				sprite.sprite =  Assets.GameSprite.values()[(Integer) info[0]].getSprite();
				sprite.afterLight = (Boolean) info[1];
				
				components.add(sprite);
			}
			
			else if(key.equals("Speed")){
				float[] info = (float[]) map.get(key);
				SpeedComponent speed = new SpeedComponent();
				speed.x = info[0];
				speed.y = info[1];
				speed.active = true; // we're not going to transfer that's not moving lol
				
				components.add(speed);
			}
			
			else if(key.equals("Size")){
				float[] info = (float[]) map.get(key);
				SizeComponent size = new SizeComponent();
				size.width = info[0];
				size.height = info[1];
				
				components.add(size);
			}
			
			else if(key.equals("Renderable")){
				RenderableComponent renderable = new RenderableComponent();
				components.add(renderable);
			}
			
			else if(key.equals("RelativeSpeed"));
			
			else if(key.equals("Position")){
				float[] info = (float[]) map.get(key);
				PositionComponent position = new PositionComponent();
				position.x = info[0];
				position.y = info[1];
				position.overridenByBody = info[2]==1;
				
				components.add(position);
			}
			
			else if(key.equals("Particle")){
				int info = (Integer) map.get(key);
				ParticleComponent particle = new ParticleComponent();
				particle.gameParticle = ParticleComponent.GameParticle.values()[info];
				
				components.add(particle);
			}
			
			else if(key.equals("OutOfBounds")){
				int info = (Integer) map.get(key);
				OutOfBoundsComponent outOfBounds = new OutOfBoundsComponent();
				outOfBounds.action = OutOfBoundsComponent.AdequateAction.values()[info];
			}
			
			else if(key.equals("Light")){
				Object[] info = (Object[]) map.get(key);
				LightComponent light = new LightComponent();
				light.color = (Color) info[0];
				light.distance = (Float) info[1];
				light.rays = (Integer) info[2];
				light.x = (Float) info[3];
				light.y = (Float) info[4];
				
				components.add(light);
			}
			
			else if(key.equals("Launchable"));
			
			else if(key.equals("Dispatchable"));
			
			else if(key.equals("DamageSprite")){
				int info = (Integer) map.get(key);
				DamageSpriteComponent damageSprite = new DamageSpriteComponent();
				damageSprite.damageSprite = Assets.GameSprite.values()[info].getSprite();
				
				components.add(damageSprite);
			}
			
			else if(key.equals("Coupled"));
			
			else if(key.equals("Controllable"));
			
			else if(key.equals("Body")){
				if(map.get(key)==null){
					components.add(new BodyComponent());
				}
				else{
					float [][] info = (float[][]) map.get(key);
					Vector2[] vertices = new Vector2[info.length];
					for(int i = 0; i < info.length; i++)
						vertices[i].set(info[i][0], info[i][1]);
					BodyComponent body = new BodyComponent();
					body.vertices = vertices;
					
					components.add(body);		
				}

			}
				
		}
		
		return components;
	}

}

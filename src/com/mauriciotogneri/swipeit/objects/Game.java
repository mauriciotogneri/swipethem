package com.mauriciotogneri.swipeit.objects;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import com.mauriciotogneri.swipeit.MainActivity;
import com.mauriciotogneri.swipeit.audio.AudioManager;
import com.mauriciotogneri.swipeit.input.InputEvent;
import com.mauriciotogneri.swipeit.media.Resources;
import com.mauriciotogneri.swipeit.objects.tiles.Tile;
import com.mauriciotogneri.swipeit.objects.tiles.Tile.TileType;
import com.mauriciotogneri.swipeit.objects.tiles.arrow.TileArrowDown;
import com.mauriciotogneri.swipeit.objects.tiles.arrow.TileArrowLeft;
import com.mauriciotogneri.swipeit.objects.tiles.arrow.TileArrowRight;
import com.mauriciotogneri.swipeit.objects.tiles.arrow.TileArrowUp;

public class Game
{
	public static final int RESOLUTION_X = 4;
	public static final int RESOLUTION_Y = 6;
	private static final int MAX_NUMBER_OF_TILES = Game.RESOLUTION_X * Game.RESOLUTION_Y;
	private static final int DIFFICULTY_LIMIT = 10;
	private static final int TOTAL_TIME = 60;
	
	private static final int COLOR_NORMAL = Color.argb(255, 85, 85, 85);
	private static final int COLOR_WARNING = Color.argb(255, 255, 60, 60);

	private final MainActivity activity;
	private final Renderer renderer;
	private final AudioManager audioManager;

	private float time = Game.TOTAL_TIME;
	private int score = 0;
	private int difficultyCounter = 0;
	private boolean finished = false;

	private final List<Tile> tiles = new ArrayList<Tile>();
	
	public Game(MainActivity activity, GLSurfaceView surfaceView)
	{
		this.activity = activity;
		this.renderer = new Renderer(activity, this, surfaceView);
		this.audioManager = new AudioManager(activity);
		this.audioManager.playAudio("audio/music/music.ogg");

		restart();
	}

	public void updateScore(int value)
	{
		this.score += value;

		if (this.score < 0)
		{
			this.score = 0;
		}
		
		this.activity.updateScore(this.score);
	}
	
	public void updateTimer()
	{
		int finalTime = (int)this.time;
		
		this.activity.updateTimer((finalTime > 9) ? String.valueOf(finalTime) : ("0" + finalTime), (finalTime > 9) ? Game.COLOR_NORMAL : Game.COLOR_WARNING);
		
		if ((finalTime == 0) && (!this.finished))
		{
			this.finished = true;
			this.activity.showFinalScore(this.score);
		}
	}

	public Renderer getRenderer()
	{
		return this.renderer;
	}

	public void restart()
	{
		this.time = Game.TOTAL_TIME;
		this.score = 0;
		this.difficultyCounter = 0;
		this.finished = false;
		this.tiles.clear();
		
		createNewTile();
		updateScore(0);
		updateTimer();
	}

	private void createNewTile()
	{
		Tile initialTile = getNewTile();
		this.tiles.add(initialTile);
	}

	private Tile getNewTile()
	{
		Random random = new Random();

		TileType type = TileType.values()[random.nextInt(TileType.values().length)];

		int i = random.nextInt(Game.RESOLUTION_X);
		int j = random.nextInt(Game.RESOLUTION_Y);

		while (tileOccupied(i, j))
		{
			i = random.nextInt(Game.RESOLUTION_X);
			j = random.nextInt(Game.RESOLUTION_Y);
		}

		return createTile(type, i, j);
	}

	private boolean tileOccupied(int i, int j)
	{
		boolean result = false;

		for (Tile tile : this.tiles)
		{
			if (tile.isIn(i, j))
			{
				result = true;
				break;
			}
		}

		return result;
	}

	private Tile createTile(TileType type, int i, int j)
	{
		Tile result = null;

		switch (type)
		{
			case UP:
				result = new TileArrowUp(i, j);
				break;
			case DOWN:
				result = new TileArrowDown(i, j);
				break;
			case LEFT:
				result = new TileArrowLeft(i, j);
				break;
			case RIGHT:
				result = new TileArrowRight(i, j);
				break;
		}
		
		return result;
	}
	
	public void update(float delta, int positionLocation, int colorLocation, InputEvent input)
	{
		if (!this.finished)
		{
			this.time -= delta;
			updateTimer();

			processInput(input);
			
			for (Tile tile : getTileList())
			{
				tile.update(delta);
				processTile(tile);
				tile.draw(positionLocation, colorLocation);
			}
		}
	}

	private Tile[] getTileList()
	{
		Tile[] result = new Tile[this.tiles.size()];
		this.tiles.toArray(result);

		return result;
	}
	
	private void processInput(InputEvent input)
	{
		if (input.isValid())
		{
			Tile tile = getTile(input.x, input.y);
			
			if (tile != null)
			{
				if (tile.acceptsInput(input.type))
				{
					tile.process(input.type);
				}
			}
		}
	}

	private void processTile(Tile tile)
	{
		if (tile.isSwipedOk())
		{
			removeAndCreate(tile);
			
			updateScore(1);

			playSound(Resources.Sounds.SWIPE_OK);
			
			increaseDifficulty();
		}
		else if (tile.isSwipedFail())
		{
			removeAndCreate(tile);

			updateScore(-1);
			
			this.activity.vibrate();
		}
		else if (tile.isTimeOut())
		{
			removeAndCreate(tile);

			updateScore(-1);
			
			playSound(Resources.Sounds.SWIPE_FAIL);
		}
	}
	
	private void removeAndCreate(Tile tile)
	{
		this.tiles.remove(tile);
		createNewTile();
	}

	private void increaseDifficulty()
	{
		this.difficultyCounter++;

		if (this.difficultyCounter == Game.DIFFICULTY_LIMIT)
		{
			this.difficultyCounter = 0;

			if (this.tiles.size() < Game.MAX_NUMBER_OF_TILES)
			{
				createNewTile();
			}
		}
	}
	
	private void playSound(String soundPath)
	{
		this.audioManager.playSound(soundPath);
	}
	
	private Tile getTile(int x, int y)
	{
		Tile result = null;

		for (Tile tile : this.tiles)
		{
			if (tile.isIn(x, y))
			{
				result = tile;
				break;
			}
		}

		return result;
	}
	
	public void resume()
	{
		if (this.audioManager != null)
		{
			this.audioManager.resumeAudio();
		}
	}
	
	public void pause(boolean finishing)
	{
		if (this.audioManager != null)
		{
			this.audioManager.pauseAudio();
		}
		
		if (this.renderer != null)
		{
			this.renderer.pause(finishing);
		}
	}

	public void stop()
	{
		if (this.audioManager != null)
		{
			this.audioManager.stopAudio();
		}
	}
}
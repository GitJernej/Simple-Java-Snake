import java.util.ArrayList;
import java.util.Random;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class Game{
	JFrame frame;
	DrawPanel drawPanel;
	
	Snake snake;
	Food food;
	ScoreData scoreDate;
	
	ArrayList<ScoreData> scoreData = new ArrayList<>();
		
	int points;
	
	String playerName;	
	
	boolean nameSaved = false;
	boolean saveName = false;
	
	boolean newGame = false;
	
	boolean scoreSorted = false;
	boolean scoreHaveData = false;
	
	int wtime = 250;
	int menuChoice = 0;
		
	boolean running = true;
	boolean moved = false;
	boolean collision = false;
	
	enum State{MENU, GAME, PAUSED, GAME_OVER, SCORE}
	State state = State.MENU;
	
	final int FRAME_WIDTH = 275;
	final int FRAME_HEIGHT = 220;
	final String SAVED_SCORES = "score.txt";	
	
	// Main metoda
	public static void main(String[] args){
		new Game().init();
	}
	
	// Inicializacija okna, zaslona... 
	// Kliče se metoda run()
	private void init(){
		frame = new JFrame("Snake Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		drawPanel = new DrawPanel();
		
		frame.getContentPane().add(BorderLayout.CENTER, drawPanel);
		frame.setVisible(true);
		frame.setResizable(false);
		
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		frame.setLocation(250, 150);
		
		frame.addKeyListener(new CustomKeyListener());

		run();
	}
	
	// Glavni del programa
	private void run(){		
		while(running){
			switch(state){
				case MENU:
					newGame = true;				
				break;
				case GAME:
					// if we started a new game, prepare stuff, and turn newGame to false
					if(newGame){
						points = 16;
						playerName = "";
						snake = new Snake(31,31);
						food = new Food(randomNumber(),randomNumber());
						snake.snakeBody().add(snake.snakeHead());
						newGame = false;
					}
					// move head
					if(snake.snakeUp()){
						snake.snakeHead(snake.snakeHead().x(),snake.snakeHead().y()-10);
					}else if(snake.snakeDown()){
						snake.snakeHead(snake.snakeHead().x(),snake.snakeHead().y()+10);
					}else if(snake.snakeLeft()){
						snake.snakeHead(snake.snakeHead().x()-10,snake.snakeHead().y());
					}else if(snake.snakeRight()){
						snake.snakeHead(snake.snakeHead().x()+10,snake.snakeHead().y());
					}					
					snake.snakeBody().add(snake.snakeHead());
					
					
					// collision in food
					if(snake.snakeHead().x() == food.food().x() && 
					snake.snakeHead().y() == food.food().y()){
						food.foodNew(randomNumber(), randomNumber());
						points+=10*snake.snakeBody().size();
					}
					else{ // else we remove tail
						snake.snakeBody().remove(0);
						if(snake.snakeUp || snake.snakeDown || snake.snakeLeft || snake.snakeRight)
							points--;
					}
					// check fatal collision and if true, it's game over!
					if(fatalCollision(snake))
							state = State.GAME_OVER;
								
				break;
				case PAUSED:
				// do nothing...
				break;
				case GAME_OVER:
				// save the player and sort scores
					if(saveName){
						savePlayerName();
						scoreSorted = false;
					}
				break;
				case SCORE:
				// if we dont have score data, we need to get it 
					if(!scoreHaveData)
						scoreGetData();
				break;
			}
			frame.repaint();
			try{ 
			Thread.sleep(wtime);
			} catch (Exception e) {}
		}
	}
	
	public void scoreGetData(){
		try(BufferedReader br = new BufferedReader(new FileReader(SAVED_SCORES))){
			scoreData = new ArrayList<>();
			String[] parts;
			
			while(br.ready()){
				parts = br.readLine().split(":");
				scoreData.add(new ScoreData(parts[0], Integer.parseInt(parts[1])));
			}
			br.close();
		}catch(IOException e) {}
	}
	
	// sorts the score and cuts if there are more than 10 players, result is saved and overwrites the (SAVED_SCORES).txt
	public void sortScore(){
		try(BufferedReader br = new BufferedReader(new FileReader(SAVED_SCORES))){
			ArrayList<ScoreData> scoreDataTemp = new ArrayList<>();
			ArrayList<ScoreData> temp = new ArrayList<>();
			
			String[] parts;
			
			while(br.ready()){
				parts = br.readLine().split(":");
				scoreDataTemp.add(new ScoreData(parts[0], Integer.parseInt(parts[1])));
			}
			br.close();
			
			for(int i = 0; i<scoreDataTemp.size()-1; i++){
				for(int j = 1; j < scoreDataTemp.size()-i; j++){
					if(scoreDataTemp.get(j-1).score() < scoreDataTemp.get(j).score()){
						temp.add(0, scoreDataTemp.get(j-1));
						scoreDataTemp.set(j-1, scoreDataTemp.get(j));
						scoreDataTemp.set(j, temp.get(0));
						temp.clear();
					}
				}
			}
			
			if(scoreDataTemp.size() > 10){
				scoreDataTemp.subList(10, scoreDataTemp.size()).clear();
			}
			
			scoreDataTemp.trimToSize();
			
			scoreSorted = true;
			scoreData = scoreDataTemp;
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(SAVED_SCORES));
			for(ScoreData data : scoreData){
				bw.write(data.name() + ":" + data.score());
				bw.newLine();
			}
			bw.close();
			
			
		}catch (IOException e) {}
	}
	
	// shrani ime in točke, ločena z dvopičjem ":", v svojo vrstico
	// tudi sortira
	public void savePlayerName(){
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(SAVED_SCORES, true))) {
		bw.write(playerName + ":" + points);
		bw.newLine();
		bw.close();		
		nameSaved = true;
		}catch (IOException e) {}		
		saveName = false;
		
		if(nameSaved){
			state = State.SCORE;
			nameSaved = false;				
		}
		sortScore();
	}
	// preveri smrtonosne trke - glava v rep, glava v zid
	public boolean fatalCollision(Snake snake){
		// collision in itself
		for (int i = 0; i < snake.snakeBody().size() - 3; i++){
			if((snake.snakeHead().x() == snake.snakeBody().get(i).x()) &&
			(snake.snakeHead().y() == snake.snakeBody().get(i).y())){
				return true;						
			}
		}		
		// collision in border
		if(snake.snakeHead().x() < 21 || snake.snakeHead().x() >171)
			return true;
		if(snake.snakeHead().y() < 21 || snake.snakeHead().y() >171)
			return true;
		
		return false;
		
	}
	
	// vrne vrednost znotraj igralnega polja
	public int randomNumber(){
		return (int) (Math.random()*14+2) * 10+1;
	}
	
	
	  /***********/
	 /* RAZREDI */
	/***********/
	
	// Poslušanje
	class CustomKeyListener implements KeyListener{
		
		int key;
		public void keyTyped(KeyEvent e){}
		public void keyPressed(KeyEvent e){
			key = e.getKeyCode();
			
			switch(state){
				case MENU:
					if(key == KeyEvent.VK_UP)
						menuChoice--;
					if(key == KeyEvent.VK_DOWN)
						menuChoice++;
					if(menuChoice > 2) menuChoice = 0;
					if(menuChoice < 0) menuChoice = 2;
					
					if(key == KeyEvent.VK_ENTER)
						switch(menuChoice){
							case 0:
								state = State.GAME;
							break;
							case 1:
								state = State.SCORE;
							break;
							case 2:
								frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
							break;
						}
					
					
					
				break;
				case GAME:
					if(moved){
						moved = false;
						if(key == KeyEvent.VK_W || key == KeyEvent.VK_UP){
							if(!snake.snakeDown){
								snake.snakeUp(true);
								snake.snakeDown(false);
								snake.snakeLeft(false);
								snake.snakeRight(false);
							}
						}
						if(key == KeyEvent.VK_S || key == KeyEvent.VK_DOWN){
							if(!snake.snakeUp){
								snake.snakeUp(false);
								snake.snakeDown(true);
								snake.snakeLeft(false);
								snake.snakeRight(false);
							}
						}
						if(key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT){
							if(!snake.snakeRight){
								snake.snakeUp(false);
								snake.snakeDown(false);
								snake.snakeLeft(true);
								snake.snakeRight(false);
							}
						}
						if(key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT){
							if(!snake.snakeLeft){
								snake.snakeUp(false);
								snake.snakeDown(false);
								snake.snakeLeft(false);
								snake.snakeRight(true);
							}
						}
					}
					if(key == KeyEvent.VK_P)
						state = State.PAUSED;
				break;
				case PAUSED:
					if(key == KeyEvent.VK_P)
						state = State.GAME;
					if(key == KeyEvent.VK_Q)
						state = State.GAME_OVER;
				break;
				case GAME_OVER:
					// if key is 0-9 or A-Z or SPACE - append it to the string
					if(key >= KeyEvent.VK_0 && key <= KeyEvent.VK_9 || key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z || key == KeyEvent.VK_SPACE)
						playerName += Character.toString( (char)key);
					// else if key is backspace and there is anything to remove in string, remove last char in string
					else if(key == KeyEvent.VK_BACK_SPACE && playerName.length() > 0)
						playerName = playerName.substring(0,playerName.length()-1);
					if(key == KeyEvent.VK_ENTER || playerName.length() > 15){
						saveName = true;					
					}
				break;
				case SCORE:
					if(key == KeyEvent.VK_ENTER)
						state = State.MENU;
				break;
			}
		}
		public void keyReleased(KeyEvent e){}
	}
	
	// Risanje
	class DrawPanel extends JPanel{
		public void paintComponent(Graphics g){
			switch(state){
				case MENU:
					paintMenu(g);
				break;
				case GAME:
					paintGame(g);
				break;
				case PAUSED:
					paintGame(g);
					paintPaused(g);
				break;
				case GAME_OVER:
					paintGame(g);
					paintGameOver(g);
				break;
				case SCORE:
					paintScore(g);				
				break;
			}
		}		
		public void paintMenu(Graphics g){
			g.setColor(Color.BLACK);
			g.fillRect(0,0, this.getWidth(), this.getHeight());
			
			g.setColor(Color.WHITE);
			g.drawRect(-1,this.getHeight()/2 - 26, this.getWidth(), 43);
			switch(menuChoice){
				case 0:
					g.setColor(Color.WHITE);
					g.fillRect(this.getWidth()/2 - 25,this.getHeight()/2 - 25, 55, 12);
					
					g.setColor(Color.BLACK);
					g.drawString("START",this.getWidth()/2 - 20,this.getHeight()/2 - 15);
					g.setColor(Color.WHITE);
					g.drawString("SCORE",this.getWidth()/2 - 20,this.getHeight()/2);
					
					g.drawString("QUIT",this.getWidth()/2 - 16,this.getHeight()/2 +15);
				break;
				case 1:
					g.setColor(Color.WHITE);
					g.drawString("START",this.getWidth()/2 - 20,this.getHeight()/2 - 15);
					
					g.fillRect(this.getWidth()/2 - 25,this.getHeight()/2 - 10, 55, 12);
					g.setColor(Color.BLACK);
					g.drawString("SCORE",this.getWidth()/2 - 20,this.getHeight()/2);
					
					g.setColor(Color.WHITE);
					g.drawString("QUIT",this.getWidth()/2 - 16,this.getHeight()/2 +15);
				break;
				case 2:
					g.setColor(Color.WHITE);
					g.drawString("START",this.getWidth()/2 - 20,this.getHeight()/2 - 15);
					g.drawString("SCORE",this.getWidth()/2 - 20,this.getHeight()/2);
					
					g.fillRect(this.getWidth()/2 - 25,this.getHeight()/2 +5, 55, 12);
					g.setColor(Color.BLACK);
					g.drawString("QUIT",this.getWidth()/2 - 16,this.getHeight()/2 +15);
				break;
			}
			

			
		}
		public void paintGame(Graphics g){
			g.setColor(Color.BLACK);
			g.fillRect(0,0, this.getWidth(), this.getHeight());
			
			g.setColor(Color.WHITE);
			
			g.drawString("Score: " + points, 200, 40);
			g.drawString("Length: " + snake.snakeBody().size(), 200, 60);
			
			g.drawRect(20,20, 160, 160);
			
			g.setColor(Color.GREEN);
			g.fillRect(food.food().x(), food.food().y(), 9, 9); 
			
			g.setColor(Color.RED);
			for(int i = 0; i < snake.snakeBody().size(); i++)
				g.fillRect(snake.snakeBody().get(i).x(), snake.snakeBody().get(i).y(), 9, 9);
				
			moved = true;
			
			
			
		}
		public void paintPaused(Graphics g){
			g.setColor(Color.WHITE);
			g.fillRect(0,0, this.getWidth(), this.getHeight()/10);
			
			g.setColor(Color.BLACK);
			g.drawString("PAUSED (P)",this.getWidth()/2 - 40, 15);
		}
		public void paintGameOver(Graphics g){
			g.setColor(Color.WHITE);
			g.fillRect(50, this.getHeight()/2, 200, 20);
			
			g.setColor(Color.BLACK);
			g.drawString("YOUR NAME: " + playerName, 55, this.getHeight()/2+15);
		}
		public void paintScore(Graphics g){
			g.setColor(Color.BLACK);
			g.fillRect(0,0, this.getWidth(), this.getHeight());
			
			g.setColor(Color.WHITE);
			for(int i = 0, offset = 20; i < scoreData.size(); i++, offset+=15){
				g.drawString((i+1) + ".", 30, offset);
				g.drawString(scoreData.get(i).name(), 50, offset);
				g.drawString(scoreData.get(i).scoreString(), 150, offset);
			}
			
			g.fillRect(0,this.getHeight()-20, this.getWidth(), this.getHeight());
			g.setColor(Color.BLACK);
			g.drawString("Press Enter to continue", 55, this.getHeight()-5);
			
		}		
	}
	// Kača - snakeBody(), snakeUp(), snakeDown(), snakeLeft8), snakeRight(),
	//		- snakeUp(boolean), snakeDown(boolean), 
	//		- snakeLeft(boolean), snakeRight(boolean)
	class Snake{
		RectData snakeHead;
		
		boolean snakeUp = false;
		boolean snakeDown = false;
		boolean snakeLeft = false;
		boolean snakeRight = false;
		
		ArrayList<RectData> snakeBody = new ArrayList<>();
		
		Snake(int x, int y){
			snakeHead  = new RectData(x, y);
		}
		
		public void snakeHead(int x, int y){
			snakeHead  = new RectData(x, y);
		}
		public RectData snakeHead(){
			return snakeHead;
		}
		
		public ArrayList<RectData> snakeBody(){
			return snakeBody;
		}
		
		public boolean snakeUp(){
			return snakeUp;
		}
		public boolean snakeDown(){
			return snakeDown;
		}
		public boolean snakeLeft(){
			return snakeLeft;
		}
		public boolean snakeRight(){
			return snakeRight;
		}
		public void snakeUp(boolean snakeUp){
			this.snakeUp = snakeUp;
		}
		public void snakeDown(boolean snakeDown){
			this.snakeDown = snakeDown;
		}
		public void snakeLeft(boolean snakeLeft){
			this.snakeLeft = snakeLeft;
		}
		public void snakeRight(boolean snakeRight){
			this.snakeRight = snakeRight;
		}
	}
	// Hrana - RectData food, foodExists(), foodExists(boolean)
	class Food{
		RectData food;
		
		Food(int x, int y){
			food = new RectData(x, y);
		}		
		public RectData food(){
			return food;
		}
		
		public void foodNew(int x, int y){
			food = new RectData(x, y);
		}
	}
	// Lokacije kvadrata - x(), y(), x(int), y(int)
	class RectData{
		int x;
		int y;
		
		RectData(){
			x = 0;
			y = 0;
		}
		
		RectData(int x, int y){
			this.x = x;
			this.y = y;
		}
		public int x(){
			return x;
		}
		public int y(){
			return y;
		}
		public void x(int x){
			this.x=x;
		}
		public void y(int y){
			this.y=y;
		}
	}
	// Podatki za točke - name(), score(), scoreString(), name(String), score(int)
	class ScoreData{
		String name;
		int score;
		
		ScoreData(String name, int score){
		this.name = name;
		this.score = score;
		}		
		public String name(){
			return name;
		}
		public int score(){
			return score;
		}
		public String scoreString(){
			return Integer.toString(score);
		}
		public void name(String name){
			this.name=name;
		}
		public void score(int score){
			this.score=score;
		}
	}
}
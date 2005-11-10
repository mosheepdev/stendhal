/* $Id$ */
/***************************************************************************
 *                      (C) Copyright 2003 - Marauroa                      *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package games.stendhal.client.gui;

import games.stendhal.client.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferStrategy;
import java.net.URL;

import javax.swing.*;

import marauroa.common.Log4J;
import marauroa.common.game.AttributeNotFoundException;
import marauroa.common.game.RPObject;

import org.apache.log4j.Logger;


/** The main class that create the screen and starts the arianne client. */
public class j2DClient extends JFrame
  {
  private static final long serialVersionUID = 3356310866399084117L;
  /** height of the chat line */
  private static final int CHAT_LINE_SIZE = 20;
  /** width of the game screen (without the chat line) */
  private static final int SCREEN_WIDTH = 640;
  /** height of the game screen (without the chat line) */
  private static final int SCREEN_HEIGHT = 480;


  /** the logger instance. */
  private static final Logger logger = Log4J.getLogger(j2DClient.class);
  
  private GameScreen screen;
  private InGameGUI inGameGUI;

  private boolean gameRunning=true;

  private StendhalClient client;


  /** NOTE: It sounds bad to see here a GUI component. Try other way. */
  private JTextField playerChatText;

  public j2DClient(StendhalClient sc)
    {
    super();
    this.client=sc;

    // create a frame to contain our game
    setTitle("Stendhal "+stendhal.VERSION+" - a multiplayer online game using Arianne");

    URL url = this.getClass().getClassLoader().getResource("data/StendhalIcon.png");
    this.setIconImage(new ImageIcon(url).getImage());

    // get hold the content of the frame and set up the resolution of the game
    JPanel panel = (JPanel) this.getContentPane();
    panel.setPreferredSize(new Dimension(SCREEN_WIDTH,SCREEN_HEIGHT+CHAT_LINE_SIZE));
    panel.setLayout(null);

    // setup our canvas size and put it into the content of the frame
    Canvas canvas=new Canvas();
    canvas.setBounds(0,0,SCREEN_WIDTH,SCREEN_HEIGHT);
    // Tell AWT not to bother repainting our canvas since we're
    // going to do that our self in accelerated mode
    canvas.setIgnoreRepaint(true);
    panel.add(canvas);

    playerChatText=new JTextField("");
    playerChatText.setBounds(0,SCREEN_HEIGHT,SCREEN_WIDTH,CHAT_LINE_SIZE);

    StendhalChatLineListener chatListener=new StendhalChatLineListener(client,playerChatText);
    playerChatText.addActionListener(chatListener);
    playerChatText.addKeyListener(chatListener);
    panel.add(playerChatText);


    this.setLocation(new Point(100, 100));

      // finally make the window visible
    pack();
    setResizable(false);
    setVisible(true);

    // add a listener to respond to the user closing the window. If they
    // do we'd like to exit the game
    addWindowListener(new WindowAdapter()
      {
      public void windowClosing(WindowEvent e)
        {
        gameRunning=false;
        }
      });

    canvas.addFocusListener(new FocusListener()
      {
      public void focusGained(FocusEvent e)
        {
        playerChatText.requestFocus();
        }

      public void focusLost(FocusEvent e)
        {
        }
      });

    addFocusListener(new FocusListener()
      {
      public void focusGained(FocusEvent e)
        {
        playerChatText.requestFocus();
        }

      public void focusLost(FocusEvent e)
        {
        }
      });

    // request the focus so key events come to us
    playerChatText.requestFocus();
    requestFocus();

    // create the buffering strategy which will allow AWT
    // to manage our accelerated graphics
    BufferStrategy strategy;
    canvas.createBufferStrategy(2);
    strategy = canvas.getBufferStrategy();

    GameScreen.createScreen(strategy,SCREEN_WIDTH,SCREEN_HEIGHT);
    screen = GameScreen.get();
    screen.setComponent(canvas);

    inGameGUI=new InGameGUI(client);

    canvas.addMouseListener(inGameGUI);
//    canvas.addMouseMotionListener(inGameGUI);

    // add a key input system (defined below) to our canvas so we can respond to key pressed
    playerChatText.addKeyListener(inGameGUI);

    client.setGameLogDialog(new GameLogDialog(this, playerChatText));

    addComponentListener(new ComponentAdapter()
      {
      public void componentHidden(ComponentEvent e)
        {
        }
      public void componentMoved(ComponentEvent e)
        {
        Dimension size=getSize();
        Point location=getLocation();

        client.getGameLogDialog().setLocation(new Point((int)location.getX(), (int)(location.getY()+size.getHeight())));
        }

      public void componentResized(ComponentEvent e)
        {
        }

      public void componentShown(ComponentEvent e)
        {
        }
      });


    // Start the main game loop, note: this method will not
    // return until the game has finished running. Hence we are
    // using the actual main thread to run the game.
    gameLoop();
    }

  public void gameLoop()
    {
    long lastLoopTime = System.currentTimeMillis();
    int fps=0;

    StaticGameLayers staticLayers=client.getStaticGameLayers();
    GameObjects gameObjects=client.getGameObjects();

    long oldTime=System.nanoTime();
    
    // Clear the first screen with black color
    screen.expose().setColor(Color.black);
    screen.expose().fill(new Rectangle(0,0,SCREEN_WIDTH,SCREEN_HEIGHT));

    screen.place(-100,-100);
    RenderingPipeline pipeline=RenderingPipeline.get();
    pipeline.addGameLayer(staticLayers);
    pipeline.addGameObjects(gameObjects);

    // keep looping round til the game ends
    while (gameRunning)
      {
      fps++;
      // work out how long its been since the last update, this
      // will be used to calculate how far the entities should
      // move this loop
      long delta = System.currentTimeMillis() - lastLoopTime;
      lastLoopTime = System.currentTimeMillis();

      logger.debug("Move objects");
      gameObjects.move(delta);

      logger.debug("Draw screen");
      pipeline.draw(screen);
      inGameGUI.draw(screen);

      screen.nextFrame();

      logger.debug("Query network");
      client.loop(0);

      logger.debug("Move screen");
      moveScreen(client.getPlayer(),staticLayers,delta);

      if(System.nanoTime()-oldTime>1000000000)
        {
        oldTime=System.nanoTime();
        logger.info("FPS: "+Integer.toString(fps));
        fps=0;
        }

      gameRunning&=client.shouldContinueGame();
      
      logger.debug("Start sleeping");
      long wait=20-delta;
      if(wait>0)
        {
        if(wait>50) 
          {
          logger.info("Waiting "+wait+" ms");
          wait=50;
          }
          
        try{Thread.sleep(wait);}catch(Exception e){};
        }
        
      logger.debug("End sleeping");
      }

    logger.info("Request logout");
    client.logout();

    logger.debug("Exit");
    System.exit(0);
    }

  private void moveScreen(RPObject object, StaticGameLayers gameLayers, long delta)
    {
    try
      {
      if(object==null)
        {
        return;
        }

      double x=object.getDouble("x");
      double y=object.getDouble("y");

      GameScreen screen=GameScreen.get();
      double screenx=screen.getX();
      double screeny=screen.getY();
      double screenw=screen.getWidth();
      double screenh=screen.getHeight();
      double sdx=screen.getdx();
      double sdy=screen.getdy();

      double dsx=screenx+screenw/2;
      double dsy=screeny+screenh/2;

      if(dsx-x<-2)
        {
        sdx+=0.2;
        }
      else if(dsx-x>-0.5 && dsx-x<0.5)
        {
        sdx/=1.3;
        }
      else if(dsx-x>2)
        {
        sdx-=0.2;
        }


      if(dsy-y<-2)
        {
        sdy+=0.2;
        }
      else if(dsy-y>-0.5 && dsy-y<0.5)
        {
        sdy/=1.3;
        }
      else if(dsy-y>2)
        {
        sdy-=0.2;
        }

      screen.move(sdx,sdy);
      }
    catch(AttributeNotFoundException e)
      {
      //Logger.thrown("j2DClient::moveScreen","X",e);
      }
    }

  public static void main(String args[])
    {
    if(args.length>0)
      {
      int i=0;
      String username=null;
      String password=null;
      String host=null;

      while(i!=args.length)
        {
        if(args[i].equals("-u"))
          {
          username=args[i+1];
          }
        else if(args[i].equals("-p"))
          {
          password=args[i+1];
          }
        else if(args[i].equals("-h"))
          {
          host=args[i+1];
          }
        i++;
        }

      if(username!=null && password!=null && host!=null)
        {
        StendhalClient client=StendhalClient.get();
        try
          {
          client.connect(host,32160);
          client.login(username,password);

          new j2DClient(client).setVisible(true);
          }
        catch(Exception ex)
          {
          ex.printStackTrace();
          }

        return;
        }
      }

    System.out.println("Stendhal j2DClient\n");
    System.out.println("  games.stendhal.j2DClient -u username -p pass -h host -c character\n");
    System.out.println("Required parameters");
    System.out.println("* -h\tHost that is running Marauroa server");
    System.out.println("* -u\tUsername to log into Marauroa server");
    System.out.println("* -p\tPassword to log into Marauroa server");
    }
  }

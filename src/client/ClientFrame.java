package client;

import util.User;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;


public class ClientFrame extends JFrame{

    private JTextField ipField;//ip文本框
    private JTextField portField;//端口文本框
    private JTextField userNameField;//用户名文本框
    private JButton connBtn;//连接按钮
    private JButton unConnBtn;//断开连接按钮
    private JList<User> userList;//用户列表
    private JTextArea recordArea;//消息列表
    private JTextField msgField;//消息框
    private JButton sendBtn;//发送按钮
    private DefaultListModel<User> userModel;
    private int frameWidth = 650;
    private int frameHeight = 500;

    private String ip = "localhost";
    private int port = 8888;
    private String userName;

    private Socket client;//客户端socket、
    private BufferedReader in;
    private PrintWriter out;
    private RecieveThread rt;//客户端负责接受信息的线程


    public ClientFrame()  {
        this("客户端程序");

    }

    public ClientFrame(String title) {
        super(title);
        init();
    }

    private void init(){
        /**
         * 初始化窗体
         */
        this.setSize(frameWidth, frameHeight);
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension d = kit.getScreenSize();
        this.setLocation((d.width-frameWidth)/2, (d.height-frameHeight)/2);

        /**
         * 初始化组件
         */
        ipField = new JTextField("localhost", 8);
        portField = new JTextField("8888", 6);
        userNameField = new JTextField("张三", 6);
        connBtn = new JButton("连接");
        unConnBtn = new JButton("断开");
        unConnBtn.setEnabled(false);

        userList = new JList<User>();
        userModel = new DefaultListModel<User>();
        userList.setModel(userModel);

        recordArea = new JTextArea();
        recordArea.setEditable(false);

        msgField = new JTextField();
        sendBtn = new JButton("发送");
        sendBtn.setEnabled(false);

        /**
         * 创建放置在内容面板北面的JPanel
         */
        JPanel northPanel = new JPanel();
        northPanel.setBorder(new TitledBorder("配置信息"));

        northPanel.add(new JLabel("服务器IP："));
        northPanel.add(ipField);
        northPanel.add(new JLabel("服务器端口："));
        northPanel.add(portField);
        northPanel.add(new JLabel("用户名："));
        northPanel.add(userNameField);
        northPanel.add(connBtn);
        northPanel.add(unConnBtn);

        /**
         * 创建放置在内容面板中间的JSplitPane
         */
        JSplitPane centerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerPane.setDividerLocation(180);

        JScrollPane leftPane = new JScrollPane(userList);
        leftPane.setBorder(new TitledBorder("用户列表"));

        JScrollPane rightPane = new JScrollPane(recordArea);
        rightPane.setBorder(new TitledBorder("消息列表"));

        centerPane.setLeftComponent(leftPane);
        centerPane.setRightComponent(rightPane);

        /**
         * 放置到内容面板南边的JPanel
         */
        JPanel southPanel = new JPanel();
        southPanel.setBorder(new TitledBorder("消息发送"));

        BorderLayout layout = new BorderLayout();
        southPanel.setLayout(layout);

        southPanel.add(msgField);
        southPanel.add(sendBtn,BorderLayout.EAST);


        /**
         * 获取内容面板
         */
        Container contentPane = this.getContentPane();
        contentPane.add(northPanel, BorderLayout.NORTH);
        contentPane.add(centerPane, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);

        this.setVisible(true);

        /**
         * 为连接按钮添加监听器
         * 1、实现创建客户端Socket
         * 2、发送上线消息给服务器
         * 3、创建一个线程 负责接收其他客户端通过服务器转发来的消息
         */
        connBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //连接服务器
                boolean flag = connect();
                if(flag){
                    //连接成功之后 发送当前用户的上线消息到服务器上
                    String msg = "ON@"+userName;
                    sendMsg(msg);
                    //启动线程 读取服务器发送来的信息
                    rt = new RecieveThread();
                    rt.start();

                    User user = new User();
                    user.setIp(client.getInetAddress().getHostAddress());
                    user.setUserName(userName);
                    userModel.addElement(user);
                    connBtn.setEnabled(false);
                    unConnBtn.setEnabled(true);
                    sendBtn.setEnabled(true);
                }else{
                    JOptionPane.showMessageDialog(ClientFrame.this, "连接失败", "警告", JOptionPane.WARNING_MESSAGE);;
                }

            }
        });


        /**
         * 为断开连接按钮添加监听器
         */
        unConnBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                unConnect();
                connBtn.setEnabled(true);
                unConnBtn.setEnabled(false);
                sendBtn.setEnabled(false);
                //清空用户列表
                userModel.removeAllElements();
                //清空消息列表
                recordArea.setText("");
            }
        });

        /**
         * 为窗体添加一个关闭事件处理监听器
         */
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                unConnect();
                ClientFrame.this.dispose();
                System.exit(0);
            }

        });

    }

    public void unConnect(){
        //1、发送下线信息给服务器
        out.println("OFF@"+userName);
        out.flush();
        //2、关闭接收信息的线程
        rt.stop();
        //3、关闭输入流 和 输出流
        if(out!=null){
            out.close();
        }
        if(in!=null){
            try {
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        //4、关闭socket
        if(client!=null && !client.isClosed()){
            try {
                client.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * 接收信息的线程
     * @author Administrator
     *
     */
    private class RecieveThread extends Thread{
        public void run(){
            try {
                while (true) {
                    String msg = in.readLine();
                    //判断消息的类型：
                    if(msg.startsWith("MSG@")){//如果是普通聊天信息
                        recordArea.append(msg.substring(4)+"\r\n");
                    }else if(msg.startsWith("ON@")){//说明是其他客户端上线的信息
                        String user = msg.substring(3);
                        userModel.addElement(User.parseUser(user));
                    }else if(msg.startsWith("OFF@")){//说明其他客户端下线的消息
                        //
                        String user = msg.substring(4);
                        userModel.removeElement(User.parseUser(user));
                    }else if(msg.startsWith("MAX@")){//说明服务器已经到了上线了
                        JOptionPane.showMessageDialog(ClientFrame.this, msg.substring(4), "消息", JOptionPane.WARNING_MESSAGE);
                        closeClient();
                        break;
                    }else if(msg.startsWith("USERLIST@")){
                        String users = msg.substring("USERLIST@".length());
                        if(!users.isEmpty()){
                            String [] data = users.split(",");
                            for(String user : data){
                                userModel.addElement(User.parseUser(user));
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ClientFrame.this, "获取服务器信息失败", "警告", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * 主动关闭当前客户端的socket 输入输出流
     */
    private void closeClient(){
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }


        if (out != null) {
            out.close();
        }

        try {
            if (client != null && !client.isClosed()) {
                client.close();
            }
        } catch (IOException e) {
            // TODO: handle exception
            e.printStackTrace();
        }
    }

    /**
     * 发送信息的方法
     * @param msg
     */
    private void sendMsg(String msg){
        out.println(msg);
        out.flush();
    }

    /**
     * 连接服务器的方法
     * @return
     */
    private boolean connect(){
        ip = ipField.getText();
        userName = userNameField.getText();
        try{
            port = Integer.parseInt(portField.getText());
        }catch(NumberFormatException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "端口号必须是1024到65535之间的整数！", "警告", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            client = new Socket(ip, port);
            //读取服务器转发给当前客户端的信息
            in = new BufferedReader(  new InputStreamReader(client.getInputStream()) );
            //发送消息给服务器
            out = new PrintWriter(client.getOutputStream());
        } catch (IOException e) {

            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "连接服务器失败！", "警告", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }



}

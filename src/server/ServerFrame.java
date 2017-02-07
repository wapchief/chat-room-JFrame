package server;

import util.User;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Administrator on 2017/2/7 0007.
 */
public class ServerFrame extends JFrame {

    private JTextField maxField;//最大连接数文本框
    private JTextField portField;//服务器端口文本框
    private JButton startBtn;//启动按钮
    private JButton stopBtn;//停止按钮
    private JList<User> userList ;//用户列表
    private JTextArea recordArea;//消息列表
    private DefaultListModel<User> userModel;//用户数据模型

    private int frameWidth = 600;
    private int frameHeigth = 400;

    private int max = 30;//最大人数上限
    private int port = 8888;//服务器端口

    private ServerSocket server;//服务器
    private AcceptThread accept;//接收连接请求的线程

    private List<ForwardThread> forwardThreads;

    public ServerFrame(String title){
        super(title);
        init();
    }

    public ServerFrame(){
        this("服务器程序");
    }

    public void init(){
        /**
         * 窗体初始化设置的代码
         */
        this.setSize(frameWidth, frameHeigth);
        Toolkit kit = Toolkit.getDefaultToolkit();
        Dimension d = kit.getScreenSize();
        this.setLocation((d.width-frameWidth)/2, (d.height-frameHeigth)/2);

        /**
         * 初始化组件
         */
        maxField = new JTextField("30", 10);
        portField = new JTextField("8888", 10);
        startBtn = new JButton("启动");
        stopBtn = new JButton("停止");
        //禁用停止按钮
        stopBtn.setEnabled(false);
        userList = new JList<User>();
        //初始化该JList对应的Model对象
        userModel = new DefaultListModel<User>();
        userList.setModel(userModel);
        //初始化多行文本框(消息列表框)
        recordArea = new JTextArea();
        //设置消息列表不可以编辑
        recordArea.setEditable(false);

        /**
         * 创建内容面板北边的JPanel
         */
        JPanel northPanel = new JPanel();
        FlowLayout layout = new FlowLayout(FlowLayout.LEFT);
        northPanel.setLayout(layout);

        northPanel.setBorder(new TitledBorder("配置信息"));

        northPanel.add(new JLabel("最大人数："));
        northPanel.add(maxField);
        northPanel.add(new JLabel("端口号："));
        northPanel.add(portField);
        northPanel.add(startBtn);
        northPanel.add(stopBtn);

        /**
         * 创建内容面板中间区域的JSplitPane
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
         * 获取到内容面板
         */
        Container contentPane = this.getContentPane();
        contentPane.add(northPanel, BorderLayout.NORTH);
        contentPane.add(centerPane, BorderLayout.CENTER);

        /**
         * 为启动按钮添加监听器
         */
        startBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                boolean flag = startServer();
                if(flag){
                    recordArea.append("服务器启动成功!\r\n");
                    //把停止按钮启用
                    stopBtn.setEnabled(true);
                    startBtn.setEnabled(false);
                }else{
                    recordArea.append("服务器启动失败!\r\n");
                }

            }
        });

        /**
         * 设置窗体显示
         */
        this.setVisible(true);
    }

    private boolean startServer(){
        forwardThreads = new LinkedList<>();
        try{
            max = Integer.parseInt(maxField.getText());
        }catch(NumberFormatException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "最大人数上限必须是一个整数数字", "警告", JOptionPane.ERROR_MESSAGE);
            maxField.setText("");
            return false;
        }

        try{
            port = Integer.parseInt(portField.getText());
        }catch(NumberFormatException e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "端口号必须是一个1024到65535之间整数数字", "警告", JOptionPane.ERROR_MESSAGE);
            portField.setText("");
            return false;
        }

        try {
            //启动服务器
            server = new ServerSocket(port);
            //建立一个连接线程
            accept = new AcceptThread();
            accept.start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "启动服务器出错,错误信息为:"+e.getMessage(), "警告", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
    /**
     * 服务器等待连接线程类
     */
    private class AcceptThread extends Thread{
        public void run(){
            try {
                while (true) {
                    //获取连接
                    Socket socket = server.accept();
                    //判断当前连接上来的客户端是否已经超过在线人数上限
                    if(forwardThreads.size()>=max){
                        PrintWriter out = new PrintWriter(socket.getOutputStream());
                        out.println("MAX@服务器用户已经达到上限,请稍候连接……");
                        out.flush();
                        out.close();
                        if(socket!=null && !socket.isClosed()){
                            socket.close();
                        }
                        continue;
                    }
                    //当人数不到上限时
                    ForwardThread fw = new ForwardThread(socket);
                    //为当前客户端创建一个转发信息的线程
                    forwardThreads.add(fw);
                    //把线程放置到List集合中
                    //启动线程
                    fw.start();
                }
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();

            }
        }
    }

    /**
     * 转发消息的线程
     */
    private class ForwardThread extends Thread{

        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private ObjectOutputStream oos;
        private User user = new User();

        /**
         * 初始化当前客户端对应的服务器端socket的输入输出流
         * 获取到当前上线的用户的用户名和ip
         *
         * 把服务器端 之前上线的用户列表发当前客户端
         * @param socket
         * @throws IOException
         */
        public ForwardThread(Socket socket) throws IOException{
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            //读取到当前用户的名字
            String msg = in.readLine();//注意 这里读取的肯定是当前用户的上线信息 ON@
            String userName = msg.substring(3);//取出用户名
            user.setUserName(userName);//为user对象设置用户名
            user.setIp(((InetSocketAddress)socket.getRemoteSocketAddress()).getHostString());//设置ip
            userModel.addElement(user);//将用户添加到用户列表中

            //将服务器端所有的在线用户发送给当前客户端
            if(!forwardThreads.isEmpty()){
                StringBuffer sb = new StringBuffer("USERLIST@");
                for(ForwardThread fw : forwardThreads){
                    sb.append(User.formatUser(fw.user));
                    sb.append(",");
                }
                //发送当前服务器端在线用户列表到客户端
                out.println(sb.toString());
                out.flush();
            }

            sendMsg("ON@"+User.formatUser(user));//将当前上线用户的用户名和ip发送给其他客户端

        }

        public void run(){
            try {
                while (true) {
                    String msg = in.readLine();
                    if (msg.startsWith("MSG@")) {//说明当前线程对应的客户端发送来的是聊天信息

                    } else if (msg.startsWith("OFF@")) {//说明当前线程对应的客户端要下线了
                        sendMsg("OFF@"+User.formatUser(user));
                        break;
                    }

                }
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();
            }finally{
                //移除当前客户端对应的转发信息的线程
                forwardThreads.remove(this);
                //从服务器在线列表中移除当前用户信息
                userModel.removeElement(user);
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
                if(socket!=null && !socket.isClosed()){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }



    }

    private void sendMsg(String msg){
        if(!forwardThreads.isEmpty()){
            for(ForwardThread fw : forwardThreads){
                fw.out.println(msg);
                fw.out.flush();
            }
        }
    }

}


package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * @author 胡帅博
 * @date 2022/4/12 22:35
 */

public class App {

    public static void main(String[] args) throws IOException {
        new Thread(new Runnable() {
            @Override
            public void run() {
                new App().test();
            }
        }).start();
    }


    public void test(){
        int a3=3;
        int a4=4;
    }

    public void test(int a){
        int a3=3;
        int a4=4;
    }

    public void test(String a,int fefe){
        int a3=3;
        int a4=4;
    }

    public void test2(){
        for(int i = 0; i < 3;
            i++) {
            System.out.println(".");
            System.out.println('.');
        }
    }
}



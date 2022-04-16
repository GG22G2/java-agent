/**
 * @author 胡帅博
 * @date 2022/4/12 22:35
 */

public class App {

    public static void main(String[] args) {
        for(int i = 0; i < 100; i++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    new App().test2();
                }
            }).start();
        }
    }


    public void test(){
        int a1=1;
        int a2=2;

        int a3=3;

        int a4=4;


    }


    public void test2(){
        int a1=1;

        String a3="1\n"
                +"2131";

        for(int i = 0;
            i < 2;
            i++) {
            System.out.println(222);
        }
    }
}



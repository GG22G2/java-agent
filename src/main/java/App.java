/**
 * @author 胡帅博
 * @date 2022/4/12 22:35
 */

public class App {

    public static void main(String[] args) {
        for(int i = 0; i < 1; i++) {
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
        int a1=2;
        String a4 = a1+
                "";

        String a5 = new StringBuilder()
                .append(a4).toString();

        for(int i = 0;
            i < a1;
            i++) {
            System.out.println(".");
        }
    }
}



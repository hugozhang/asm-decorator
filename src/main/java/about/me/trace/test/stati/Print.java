package about.me.trace.test.stati;


import about.me.trace.test.User;

public class Print {

    public static void print(Object o,String group,String key,long expire) {
        System.out.println(o);
        System.out.println(group);
        System.out.println(key);
        System.out.println(expire);
    }

    public static void print2(String group,String key,Object o,long expire) {
        System.out.println(group);
        System.out.println(key);
        System.out.println(o);
        System.out.println(expire);
    }

    public static void main(String[] args) {
        Print.print(new User(),"","",1);
        Print.print2("","",new User(),1);
    }
}

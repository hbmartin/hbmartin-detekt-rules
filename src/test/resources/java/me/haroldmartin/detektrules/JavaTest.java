package me.haroldmartin.detektrules;

public class JavaTest {
    public void something(String msg) {
        System.out.println(msg);
    }

    public abstract static class JavaStaticTest<V> {
        public final void check(V msg) {
            System.out.println(msg.toString());
        }
    }
}


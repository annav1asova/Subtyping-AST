package com.test.privet;

import annotation_package.Subtyping;

public class TestClass {

    @Subtyping(name="metr")
    int annotatedFieldWithValue = 5;

    String fieldWithValue = "field with value";

    @Subtyping(name="first_type")
    String annotatedFieldWithoutValue = "annotated field without value";

    public static void main(String[] args) {
        @Subtyping(name="window_id")
        String localVariable = "12ac2bed892f9d93e";
    }


    private class PrivateTestClass {
        @Subtyping(name="kg")
        int fieldOfPrivateClass;

        private void find(@Subtyping(name="meter") int height, @Subtyping(name="meter") int width, int extra) {

            @Subtyping(name="window_id")
            String localVariable = "fc7df8ba452b8ae82";

//            localVariable = annotatedFieldWithoutValue;

//            @Subtyping(name="kg")
//            int a = width;

            @Subtyping(name="kg")
            int a = 10;

            a = width;

            for (@Subtyping(name="counter") int i = 0; i < 2; i++) {

            }
//            for (@Subtyping(name="counter2") int i = 0; i < 2; i++) {
//
//            }

            if (localVariable.equals("test")) {
                String y = localVariable;

            } else {

            }
        }
    }
}

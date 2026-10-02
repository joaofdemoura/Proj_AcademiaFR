package com.example.app_academiafr;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test public void useAppContext() {
        assertEquals("com.example.app_academiafr", InstrumentationRegistry.getInstrumentation().getTargetContext().getPackageName());
    }
}

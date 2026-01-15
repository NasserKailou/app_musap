package modules;

import org.jooq.DSLContext;


public interface SampleConnectionHelper {
    DSLContext connection();
}

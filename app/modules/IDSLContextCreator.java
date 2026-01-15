package modules;

import org.jooq.DSLContext;

public interface IDSLContextCreator {
    DSLContext create(String username);
}

package utils;

import org.jooq.DSLContext;

public interface IConnectionHelper {
    DSLContext connection();
}

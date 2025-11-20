package modules;

import play.data.format.Formatters;
import play.data.format.Formatters.SimpleFormatter;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * @author khalid.alkassoum@advinteck.com
 * java.time.{@link LocalDateTime} formatters, needed for form.get in controllers
 * (mapping requests variables to pojos having LocalDateTime fields)
 * 20171010 - v0.1
 */
@Singleton
public class FormattersProvider implements Provider<Formatters> {

    private final MessagesApi messagesApi;

    @Inject
    public FormattersProvider(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;

        /*
        --> this is automaticaly done upon calling new Formatterst(messagesApi) in the get() method implemented below
        register(Date.class, new Formats.DateFormatter(messagesApi));
        register(Date.class, new Formats.AnnotationDateFormatter(messagesApi));
        register(String.class, new Formats.AnnotationNonEmptyFormatter());
        registerOptional();
        */
    }

    @Override
    public Formatters get() {
        Formatters formatters = new Formatters(messagesApi);

        formatters.register(LocalDateTime.class, new SimpleFormatter<LocalDateTime>() {

            @Override
            public LocalDateTime parse(String input, Locale l) throws ParseException {
                LocalDateTime localDateTime;
                try {
                    localDateTime = LocalDateTime.parse(input);
                } catch (DateTimeParseException e){
                    throw new ParseException("No valid Input", 0);
                }
                return localDateTime;
            }
            /*
            If we were to have local taken in consideration
            remimber this
            LocalDateTime ldt = LocalDateTime.now();
            System.out.println("------------------------------------------------------------TIME IS NOW :"+ldt);
            Long epoch =ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            System.out.println("------------------------------------------------------------TIME IS epoch long:"+epoch);
            LocalDateTime ldt2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch),ZoneId.systemDefault());
            System.out.println("------------------------------------------------------------TIME IS lt2:"+ldt2);
             */
            @Override
            public String print(LocalDateTime localDateTime, Locale l) {
                return localDateTime.toString();
            }

        });

        return formatters;
    }
}

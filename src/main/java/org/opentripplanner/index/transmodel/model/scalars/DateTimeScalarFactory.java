package org.opentripplanner.index.transmodel.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

public class DateTimeScalarFactory {

    public static final String EXAMPLE_DATE_TIME = "2017-04-23T18:25:43.511+0100";

    /**
     * Milliseconds and time zone offset is _REQUIRED_ in this scalar.
     * Milliseconds will handle most cases for date and time.
     * Enforcing time zone will avoid issues with making assumptions about time zones.
     * ISO 8601 alone does not require time zone.
     */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSXXXX";

    public static final String DATE_SCALAR_DESCRIPTION = "Date time using the format: " + DATE_TIME_PATTERN + ". Example: " + EXAMPLE_DATE_TIME;

    private static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private DateTimeScalarFactory() {
    }

    public static GraphQLScalarType createMillisecondsSinceEpochAsDateTimeStringScalar(TimeZone timeZone) {
        return new GraphQLScalarType("DateTime", DATE_SCALAR_DESCRIPTION, new Coercing() {
            @Override
            public String serialize(Object input) {
                if (input instanceof Long) {
                    return ((Instant.ofEpochMilli((Long) input))).atZone(timeZone.toZoneId()).format(FORMATTER);
                }
                return null;
            }

            @Override
            public Instant parseValue(Object input) {
                return Instant.from(FORMATTER.parse((CharSequence) input));
            }

            @Override
            public Object parseLiteral(Object input) {
                if (input instanceof StringValue) {
                    return parseValue(((StringValue) input).getValue()).toEpochMilli();
                }
                return null;
            }
        });
    }

}

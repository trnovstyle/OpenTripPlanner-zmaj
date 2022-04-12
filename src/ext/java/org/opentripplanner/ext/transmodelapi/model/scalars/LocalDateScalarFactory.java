package org.opentripplanner.ext.transmodelapi.model.scalars;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class LocalDateScalarFactory {

    private static final String DOCUMENTATION =
            "Local date without a time-zone, using the ISO 8601 format: `YYYY-MM-DD`. Example: `2022-04-13`.";

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private LocalDateScalarFactory() {}

    public static GraphQLScalarType createLocalDateScalar() {
        return GraphQLScalarType.newScalar()
                .name("LocalDate")
                .description(DOCUMENTATION)
                .coercing(new Coercing<>() {
                    @Override
                    public Object serialize(Object input) throws CoercingSerializeException {
                        if (input instanceof LocalDate) {
                            return ((LocalDate) input).toString();
                        }

                        return "";
                    }

                    @Override
                    public Object parseValue(Object input) throws CoercingParseValueException {
                        try {
                            return LocalDate.from(FORMATTER.parse((CharSequence) input)).toString();
                        }
                        catch (DateTimeParseException e) {
                            throw new CoercingParseValueException(
                                    "Expected type 'LocalDate' but was '" + input + "'.");
                        }
                    }

                    @Override
                    public Object parseLiteral(Object input) throws CoercingParseLiteralException {
                        if (input instanceof StringValue) {
                            return parseValue(((StringValue) input).getValue());
                        }

                        return "";
                    }
                })
                .build();
    }

}

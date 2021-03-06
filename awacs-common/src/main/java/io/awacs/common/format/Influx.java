/**
 * Copyright 2016-2017 AWACS Project.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.awacs.common.format;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Copy from org.influxdb.influxdb-java and parse to Java 7
 *
 * @author stefan.majer@gmail.com
 */
public class Influx {

    /**
     * Create a new Point Build build to create a new Point in a fluent manner.
     *
     * @param measurement the name of the measurement.
     * @return the Builder to be able to add further Builder calls.
     */
    public static Builder measurement(final String measurement) {
        return new Builder(measurement);
    }

    public static class Point {
        private String measurement;
        private Map<String, String> tags;
        private Long time;
        private TimeUnit precision = TimeUnit.NANOSECONDS;
        private Map<String, Object> fields;
        private static final int MAX_FRACTION_DIGITS = 340;

        private static final Function<String, String> FIELD_ESCAPER = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace("\\", "\\\\").replace("\"", "\\\"");
            }
        };

        private static final Function<String, String> KEY_ESCAPER = new Function<String, String>() {
            @Override
            public String apply(String s) {
                return s.replace(" ", "\\ ").replace(",", "\\,").replace("=", "\\=");
            }
        };

        private static final ThreadLocal<NumberFormat> NUMBER_FORMATTER = new ThreadLocal<NumberFormat>() {
            @Override
            protected NumberFormat initialValue() {
                NumberFormat numberFormat = NumberFormat.getInstance(Locale.ENGLISH);
                numberFormat.setMaximumFractionDigits(MAX_FRACTION_DIGITS);
                numberFormat.setGroupingUsed(false);
                numberFormat.setMinimumFractionDigits(1);
                return numberFormat;
            }
        };

        private static final ThreadLocal<Map<String, MeasurementStringBuilder>> CACHED_STRINGBUILDERS = new ThreadLocal<Map<String, MeasurementStringBuilder>>() {
            @Override
            protected Map<String, MeasurementStringBuilder> initialValue() {
                return new HashMap<>();
            }
        };

        Point() {
        }

        /**
         * @param measurement the measurement to set
         */
        void setMeasurement(final String measurement) {
            this.measurement = measurement;
        }

        /**
         * @param time the time to set
         */
        void setTime(final Long time) {
            this.time = time;
        }

        /**
         * @param tags the tags to set
         */
        void setTags(final Map<String, String> tags) {
            this.tags = tags;
        }

        /**
         * @return the tags
         */
        Map<String, String> getTags() {
            return this.tags;
        }

        /**
         * @param precision the precision to set
         */
        void setPrecision(final TimeUnit precision) {
            this.precision = precision;
        }

        /**
         * @param fields the fields to set
         */
        void setFields(final Map<String, Object> fields) {
            this.fields = fields;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Point point = (Point) o;
            return Objects.equals(measurement, point.measurement)
                    && Objects.equals(tags, point.tags)
                    && Objects.equals(time, point.time)
                    && precision == point.precision
                    && Objects.equals(fields, point.fields);
        }

        @Override
        public int hashCode() {
            return Objects.hash(measurement, tags, time, precision, fields);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Point [name=");
            builder.append(this.measurement);
            builder.append(", time=");
            builder.append(this.time);
            builder.append(", tags=");
            builder.append(this.tags);
            builder.append(", precision=");
            builder.append(this.precision);
            builder.append(", fields=");
            builder.append(this.fields);
            builder.append("]");
            return builder.toString();
        }

        /**
         * calculate the lineprotocol entry for a single Point.
         * <p>
         * Documentation is WIP : https://github.com/influxdb/influxdb/pull/2997
         * <p>
         * https://github.com/influxdb/influxdb/blob/master/tsdb/README.md
         *
         * @return the String without newLine.
         */
        public String lineProtocol() {
            final StringBuilder sb = CACHED_STRINGBUILDERS
                    .get()
                    .computeIfAbsent(this.measurement, new java.util.function.Function<String, MeasurementStringBuilder>() {
                        @Override
                        public MeasurementStringBuilder apply(String s) {
                            return new MeasurementStringBuilder(s);
                        }
                    })
                    .resetForUse();

            concatenatedTags(sb);
            concatenatedFields(sb);
            formatedTime(sb);

            return sb.toString();
        }

        private void concatenatedTags(final StringBuilder sb) {
            for (Map.Entry<String, String> tag : this.tags.entrySet()) {
                sb.append(',')
                        .append(KEY_ESCAPER.apply(tag.getKey()))
                        .append('=')
                        .append(KEY_ESCAPER.apply(tag.getValue()));
            }
            sb.append(' ');
        }

        private void concatenatedFields(final StringBuilder sb) {
            for (Map.Entry<String, Object> field : this.fields.entrySet()) {
                Object value = field.getValue();
                if (value == null) {
                    continue;
                }

                sb.append(KEY_ESCAPER.apply(field.getKey())).append('=');
                if (value instanceof Number) {
                    if (value instanceof Double || value instanceof Float || value instanceof BigDecimal) {
                        sb.append(NUMBER_FORMATTER.get().format(value));
                    } else {
                        sb.append(value).append('i');
                    }
                } else if (value instanceof String) {
                    String stringValue = (String) value;
                    sb.append('"').append(FIELD_ESCAPER.apply(stringValue)).append('"');
                } else {
                    sb.append(value);
                }

                sb.append(',');
            }

            // efficiently chop off the trailing comma
            int lengthMinusOne = sb.length() - 1;
            if (sb.charAt(lengthMinusOne) == ',') {
                sb.setLength(lengthMinusOne);
            }
        }

        private void formatedTime(final StringBuilder sb) {
            sb.append(' ').append(TimeUnit.NANOSECONDS.convert(this.time, this.precision));
        }

        private static class MeasurementStringBuilder {
            private final StringBuilder sb = new StringBuilder(128);
            private final int length;

            MeasurementStringBuilder(final String measurement) {
                this.sb.append(KEY_ESCAPER.apply(measurement));
                this.length = sb.length();
            }

            StringBuilder resetForUse() {
                sb.setLength(length);
                return sb;
            }
        }
    }

    /**
     * Builder for a new Point.
     *
     * @author stefan.majer [at] gmail.com
     */
    public static final class Builder {
        private final String measurement;
        private final Map<String, String> tags = new TreeMap<>();
        private Long time;
        private TimeUnit precision = TimeUnit.NANOSECONDS;
        private final Map<String, Object> fields = new TreeMap<>();

        /**
         * @param measurement
         */
        Builder(final String measurement) {
            this.measurement = measurement;
        }

        /**
         * Add a tag to this point.
         *
         * @param tagName the tag name
         * @param value   the tag value
         * @return the Builder instance.
         */
        public Builder tag(final String tagName, final String value) {
            Objects.requireNonNull(tagName, "tagName");
            Objects.requireNonNull(value, "value");
            if (!tagName.isEmpty() && !value.isEmpty()) {
                tags.put(tagName, value);
            }
            return this;
        }

        /**
         * Add a Map of tags to add to this point.
         *
         * @param tagsToAdd the Map of tags to add
         * @return the Builder instance.
         */
        public Builder tag(final Map<String, String> tagsToAdd) {
            for (Map.Entry<String, String> tag : tagsToAdd.entrySet()) {
                tag(tag.getKey(), tag.getValue());
            }
            return this;
        }

        /**
         * Add a field to this point.
         *
         * @param field the field name
         * @param value the value of this field
         * @return the Builder instance.
         */
        @SuppressWarnings("checkstyle:finalparameters")
        @Deprecated
        public Builder field(final String field, Object value) {
            if (value instanceof Number) {
                if (value instanceof Byte) {
                    value = ((Byte) value).doubleValue();
                } else if (value instanceof Short) {
                    value = ((Short) value).doubleValue();
                } else if (value instanceof Integer) {
                    value = ((Integer) value).doubleValue();
                } else if (value instanceof Long) {
                    value = ((Long) value).doubleValue();
                } else if (value instanceof BigInteger) {
                    value = ((BigInteger) value).doubleValue();
                }
            }
            fields.put(field, value);
            return this;
        }

        public Builder addField(final String field, final boolean value) {
            fields.put(field, value);
            return this;
        }

        public Builder addField(final String field, final long value) {
            fields.put(field, value);
            return this;
        }

        public Builder addField(final String field, final double value) {
            fields.put(field, value);
            return this;
        }

        public Builder addField(final String field, final Number value) {
            fields.put(field, value);
            return this;
        }

        public Builder addField(final String field, final String value) {
            Objects.requireNonNull(value, "value");

            fields.put(field, value);
            return this;
        }

        /**
         * Add a Map of fields to this point.
         *
         * @param fieldsToAdd the fields to add
         * @return the Builder instance.
         */
        public Builder fields(final Map<String, Object> fieldsToAdd) {
            this.fields.putAll(fieldsToAdd);
            return this;
        }

        /**
         * Add a time to this point.
         *
         * @param timeToSet      the time for this point
         * @param precisionToSet the TimeUnit
         * @return the Builder instance.
         */
        public Builder time(final long timeToSet, final TimeUnit precisionToSet) {
            Objects.requireNonNull(precisionToSet, "precisionToSet");
            this.time = timeToSet;
            this.precision = precisionToSet;
            return this;
        }

        /**
         * Create a new Point.
         *
         * @return the newly created Point.
         */
        public Point build() {
//                Preconditions.checkNonEmptyString(this.measurement, "measurement");
//                Preconditions.checkPositiveNumber(this.fields.size(), "fields size");
            Point point = new Point();
            point.setFields(this.fields);
            point.setMeasurement(this.measurement);
            if (this.time != null) {
                point.setTime(this.time);
                point.setPrecision(this.precision);
            } else {
                point.setTime(System.currentTimeMillis());
                point.setPrecision(TimeUnit.MILLISECONDS);
            }
            point.setTags(this.tags);
            return point;
        }
    }

    private interface Function<V, T> {

        T apply(V v);
    }
}

package frostvein.sampires.remakepire.utils;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class OptionalTypeAdapter implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
        if (typeToken.getRawType() != Optional.class) return null;

        Type type = typeToken.getType();
        Type wrappedType = ((ParameterizedType) type).getActualTypeArguments()[0];

        final TypeAdapter<Object> wrappedAdapter = (TypeAdapter<Object>) gson.getAdapter(TypeToken.get(wrappedType));

        TypeAdapter<Optional<Object>> adapter = new TypeAdapter<>() {
            @Override
            public void write(JsonWriter out, Optional<Object> value) throws IOException {
                if (value != null && value.isPresent()) {
                    wrappedAdapter.write(out, value.get());
                } else {
                    out.nullValue();
                }
            }

            @Override
            public Optional<Object> read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return Optional.empty();
                }
                return Optional.ofNullable(wrappedAdapter.read(in));
            }
        };

        return (TypeAdapter<T>) adapter;
    }
}

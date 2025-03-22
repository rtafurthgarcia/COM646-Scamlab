package helper;

import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.arc.Lock;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Lock()
public class PlayerConnectionRegistry extends ConcurrentHashMap<String, String> {
}
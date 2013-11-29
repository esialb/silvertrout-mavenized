package silvertrout;

import java.util.HashMap;
import java.util.Map;

public class CaseInsensitiveHashMap<V> extends HashMap<String, V> {
	private Map<String, V> backing = new HashMap<String, V>();
	
	@Override
	public V get(Object key) {
		if(key == null)
			return backing.get(key);
		return backing.get(((String) key).toLowerCase());
	}
	
	@Override
	public V put(String key, V value) {
		super.put(key, value);
		return backing.put(key.toLowerCase(), value);
	}
}

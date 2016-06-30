/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.metacontent;

import java.util.UUID;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class MVStoreArbitraryTests
{

	public static void main(String[] args)
	{
		long temp = System.currentTimeMillis();
		MVStore s = MVStore.open("foo.test");
		System.out.println("Startup: " + (System.currentTimeMillis() - temp));
		
		 temp = System.currentTimeMillis();
		MVMap<UUID,String> map =  s.<UUID,String>openMap("test");
		MVMap<Integer, UUID> map2 =  s.<Integer, UUID>openMap("test2");
		System.out.println("OpenMaps: " + (System.currentTimeMillis() - temp));
		
		temp = System.currentTimeMillis();
		UUID a = map2.get(5);
		System.out.println(a);
		System.out.println("Time to read by key: " + (System.currentTimeMillis() - temp));
		temp = System.currentTimeMillis();
		System.out.println(map2.containsValue(a));
		System.out.println("Time to find by value: " + (System.currentTimeMillis() - temp));
		
		temp = System.currentTimeMillis();
		a = map2.get(5000000);
		System.out.println("Time to read by key: " + (System.currentTimeMillis() - temp));
		temp = System.currentTimeMillis();
		System.out.println(map2.containsValue(a));
		System.out.println("Time to find by value: " + (System.currentTimeMillis() - temp));
		
//		for (int i = 0; i < 10000000; i++)
//		{
//			UUID t = UUID.randomUUID();
//			map.put(t, i);
//			map2.put(i, t);
//		}
		
//		temp = System.currentTimeMillis();
//		final AtomicInteger ai = new AtomicInteger();
		System.out.println(map.get(UUID.fromString("36b310fc-434b-4447-9539-5e86c48383d1")));
//		map.keySet().stream().forEach(uuid -> ai.addAndGet(map.get(uuid)));
//		System.out.println("stream iterate " + (System.currentTimeMillis() - temp));
//		
//		temp = System.currentTimeMillis();
//		ai.set(0);
//		map.keySet().parallelStream().forEach(uuid -> ai.addAndGet(map.get(uuid)));
//		System.out.println("parallel  iterate " + (System.currentTimeMillis() - temp));
//
//		s.commit();
//		
//		System.out.println(s.getCacheSizeUsed());
//		System.out.println(map.size());
//		System.out.println(map2.size());
		
		
		
	}

}

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 1/29/14
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
public class ShotPredictor {
    // Map of shots.  Structure is player -> type -> list of shots
    private HashMap<String, HashMap<String, List<Shot>>> _shot_map = new HashMap<String, HashMap<String, List<Shot>>>();
    private int _k = 100;

    // This function just loads the shot into our local shot map.
    private void trainShot(Shot s)
    {
        if(!_shot_map.containsKey(s._shooter))
        {
            HashMap<String, List<Shot>> to_add = new HashMap<String, List<Shot>>();
            _shot_map.put(s._shooter, to_add);
        }

        HashMap<String, List<Shot>> this_map_type_to_list = _shot_map.get(s._shooter);

        if(!this_map_type_to_list.containsKey(s._shot_type))
        {
            List<Shot> to_add = new ArrayList<Shot>();
            this_map_type_to_list.put(s._shot_type, to_add);
        }

        List<Shot> this_list_of_shots = this_map_type_to_list.get(s._shot_type);
        this_list_of_shots.add(s);
    }

    // Just calls train shot on the incoming list.
    public void trainShots(List<Shot> incoming_list)
    {
        for(Shot s : incoming_list)
        {
            trainShot(s);
        }
    }

    // Predict shot will collect all appropriate "neighbor" shots, then use the kNearestNeighbor function to give a
    //  probability that the shot will go in.  This probability is then returned as a boolean (p > 0,5).

    public boolean predictShot(Shot shot_to_predict)
    {
        boolean ret;
        //Start collecting the neighbors to consider
        List<Shot> neighbors_to_consider = new ArrayList<Shot>();
        if(_shot_map.containsKey(shot_to_predict._shooter))
        {
            if(_shot_map.get(shot_to_predict._shooter).containsKey(shot_to_predict._shot_type))
            {
                neighbors_to_consider.addAll(_shot_map.get(shot_to_predict._shooter).get(shot_to_predict._shot_type));
//                double shot_probablility = kNearestNeighbors(_k, shot_to_predict, all_neighbors);
//                ret = shot_probablility > 0.5;
            }
        }


        // If we don't yet have enough neighbors to consider (i.e. more than k neighbors), then we'll go ahead and
        //  collect all historical shots of that type.  The logic here is that for shots by a certain player that are
        //  common (i.e. jump shots), there should be plenty of history to build on.  However, for shots that are
        //  less common (like alley-oops), it's better to consider all historical shots of that type.
        //  This bit of code adds a lot to run time, but does improve predictive capability noticeably.
        if(neighbors_to_consider.size() < _k)
        {
            for(String shooter : _shot_map.keySet())
            {
                for(String shot_type : _shot_map.get(shooter).keySet())
                {
                    if(shot_type.equals(shot_to_predict._shot_type))
                    {
                        neighbors_to_consider.addAll(_shot_map.get(shooter).get(shot_type));
                    }
                }
            }
        }

        if(neighbors_to_consider.size() > 0)
        {
            double shot_probability = kNearestNeighbors(_k, shot_to_predict, neighbors_to_consider);
            ret = shot_probability > 0.5;
        }
        else
        {
            // We've never seen this shot before.
            ret = Math.random() > 0.5;
        }

        trainShot(shot_to_predict);
        return ret;
    }

    // kNearestNeighbors uses a euclidean distance between two shots (see Shot.java for details on distance metric)
    //  to ascertain incoming_shot's k-nearest neighbors.  The probability assigned to that shot is then a naive
    //  voting approach.  Giving equal weight to that shot's k nearest neighbors, and returning the % of those neighbors
    //  that were successful shots.

    private double kNearestNeighbors(int k, Shot incoming_shot, List<Shot> all_neighbors)
    {
        // First loop through all of the shots and get a distance to this shot.  Store this in a sorted map so that
        //  when we loop through later, we can make an early exit.
        SortedMap<Double, List<Shot>> shots_with_distance = new TreeMap<Double, List<Shot>>();
        for(Shot s : all_neighbors)
        {
            double distance = incoming_shot.distanceFrom(s);
            if(shots_with_distance.containsKey(distance))
            {
                shots_with_distance.get(distance).add(s);
            }
            else
            {
                List<Shot> to_add = new ArrayList<Shot>();
                to_add.add(s);
                shots_with_distance.put(distance, to_add);
            }
        }

//        // Naive voting approach
        int num_makes = 0;
        int num_shots = 0;

        // Now loop through the k closest shots and collect % made.
        for(double d : shots_with_distance.keySet())
        {
            List<Shot> these_shots = shots_with_distance.get(d);
            for(Shot s : these_shots)
            {
                num_shots++;
                num_makes += s._shot_made ? 1 : 0;
                if(num_shots >= k)
                {
                    break;
                }
            }
            if(num_shots >= k)
            {
                break;
            }
        }

        return (double) num_makes / (double) num_shots;
    }
}

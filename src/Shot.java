/**
 * Created with IntelliJ IDEA.
 * User: jed
 * Date: 1/29/14
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Shot
{
    public String _shooter;
    public String _shot_type;
    public int _seconds_elapsed_in_game;
    public int _time_on_shot_clock;
    public int _x;
    public int _y;
    public boolean _shot_made;

    public double distanceFrom(Shot other_shot)
    {
        // Using euclidean distance for distance metric.  Dimensions are x, y and shot_clock.
        //  There are some possible optimizations to be made here, but time will tell if they are necessary.
        //  Possible improvements:
        //   "mirror" the court.  A shot from the left side of the court is pretty similar to a shot made from the
        //     exact opposite side of the court.  Perhaps handedness comes into play here.
        //     -- upon experimentation, this was a factor, but not a significant one.  To observe the difference,
        //     -- swap norm_x() with _x in the line below.
        //   normalize axes.  Specifically, time on shot clock vs x and y.  1 second might mean more or less than
        //    1 foot.  This is not accounted for right now.
        //    -- upon experimentation, this has not proven to be a major indicating factor.  Leaving the time metric
        //    --  on the same scale as distance appears to work sufficiently.

        double total_distance = 0.0;
//        total_distance += (other_shot.norm_x() - norm_x()) * (other_shot.norm_x() - norm_x());
        total_distance += (other_shot._x - _x) * (other_shot._x - _x);
        total_distance += (other_shot._y - _y) * (other_shot._y - _y);
        total_distance += (other_shot._time_on_shot_clock - _time_on_shot_clock) * (other_shot._time_on_shot_clock - _time_on_shot_clock);

        return Math.sqrt(total_distance);
    }

    // This function serves to "normalize" x a bit.  The hoop is at x = 25, so by taking the absolute value of x - 25,
    //  We are looking at the deviation from the center of the court.  This way, shots from either side of the court
    //  Are considered equally.
    //  This did not prove to be a strong indicator.
    public int norm_x()
    {
        return Math.abs(_x - 25);
    }

    public String toString()
    {
        String ret = "";
        ret += "player: " + _shooter + ", ";
        ret += "shot type: " + _shot_type + ", ";
        ret += "shot clock: " + _time_on_shot_clock + ", ";
        ret += "(x,y): (" + _x + "," + _y + "), ";
        ret += _shot_made ? "made" : "missed";

        return ret;
    }
}

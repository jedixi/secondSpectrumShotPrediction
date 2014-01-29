import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class PredictionRunner{

    static SimpleDateFormat _file_date_format = new SimpleDateFormat("yyyyMMdd");

    public static void main(String[] args)
    {
        SortedMap<Date, List<Shot>> all_shots = null;
        try {
            all_shots = loadAllShots("/home/jed/Documents/Aggregated_shot_data/");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        System.out.println("all shots loaded");

        ShotPredictor myPredictor = new ShotPredictor();

        // Setting start date to first available date.  Hard-coded because we know this.
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2006);
        cal.set(Calendar.MONTH, Calendar.OCTOBER);
        cal.set(Calendar.DAY_OF_MONTH, 30);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date curr_start_date = cal.getTime();

        cal.roll(Calendar.YEAR, 1);

        Date curr_end_date = cal.getTime();

        // Loop through valid training dates
        for(Date d : all_shots.keySet())
        {
            // if the date we're looking at is >= curr_start_date and <= curr_end_date
            if((d.after(curr_start_date) || d.equals(curr_start_date)) && (d.before(curr_end_date) || d.equals(curr_end_date)))
            {
                myPredictor.trainShots(all_shots.get(d));
            }
            else
            {
                // Since we assume that the shots go in in chronological order, we can break once we've passed our end date
                break;
            }
        }

        System.out.println("shots trained through " + curr_end_date);

        cal.roll(Calendar.DATE, 1);
        Date curr_date = cal.getTime();

        int predictions_made = 0;
        int predictions_correct = 0;

        List<Boolean> most_recent_predictions = new ArrayList<Boolean>();
        while(curr_date.before(new Date()))
        {
            List<Shot> shots_today = all_shots.get(curr_date);
            if(shots_today != null && shots_today.size() > 0)
            {
                System.out.println("running shots on " + curr_date);
                for(Shot s : shots_today)
                {
                    boolean shot_prediction = myPredictor.predictShot(s);
//                    System.out.println("Shot: " + s.toString());
//                    System.out.println("Shot prediction: " + shot_prediction);

                    predictions_made++;
                    if(shot_prediction == s._shot_made)
                    {
                        predictions_correct++;
                    }

                    if(predictions_made % 1000 == 0)
                    {

                        System.out.println("All-time accuracy: " + ((double) predictions_correct / (double) predictions_made));

                        most_recent_predictions.add(shot_prediction == s._shot_made);
                        if(most_recent_predictions.size() > 1000)
                        {
                            most_recent_predictions.remove(0);
                        }

                        int recent_predictions_correct = 0;
                        for(boolean c : most_recent_predictions)
                        {
                            if(c)
                            {
                                recent_predictions_correct++;
                            }
                        }

                        System.out.println("Last 100 correct: " + (double) recent_predictions_correct / most_recent_predictions.size());
                    }
                }
            }

            cal.setTime(new Date(cal.getTimeInMillis() + 1000*60*60*24));
            curr_date = cal.getTime();

        }

    }

    // This function will loop through the files in the specified file path, looking for only CSVs.  This assumes the
    //  CSVs are appropriately formatted, so user beware.  This will then construct a Shot object for each line in each
    //  file and return these sorted by date.

    public static SortedMap<Date, List<Shot>> loadAllShots(String filepath) throws IOException, ParseException {
        SortedMap<Date, List<Shot>> all_shots = new TreeMap<Date, List<Shot>>();
        String filename;
        File folder = new File(filepath);
        File[] listOfFiles = folder.listFiles();
//            assert listOfFiles != null;
        if(listOfFiles.length > 0) {
            // Loop through files found in path
            for (File listOfFile : listOfFiles) {
                // If it is in fact a file
                if (listOfFile.isFile()) {
                    filename = listOfFile.getName();
//                    System.out.println(filename);
                    // Want to verify it is a CSV file.
                    if (filename.endsWith(".csv") || filename.endsWith(".CSV")) {
                        Calendar cal = Calendar.getInstance();
                        String date = filename.split("\\.")[0];
                        cal.setTime(_file_date_format.parse(filename.split("\\.")[0]));
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.set(Calendar.MILLISECOND, 0);
                        Date this_date = cal.getTime();

                        if(!all_shots.containsKey(this_date))
                        {
                            List<Shot> to_add = new ArrayList<Shot>();
                            all_shots.put(this_date, to_add);
                        }

                        FileInputStream fstream = new FileInputStream(filepath + filename);
                        DataInputStream in = new DataInputStream(fstream);
                        BufferedReader br = new BufferedReader(new InputStreamReader(in));
                        int lineCount = 0;
                        String strLine;
                        int last_time_of_reset = 0;
                        int last_reset_value = 24;
                        int prev_period = 1;
                        String last_possessed_by = "";
                        while ((strLine = br.readLine()) != null) {
                            lineCount++;
                            String[] strArr = strLine.split(",");
                            // Make sure we have the right number of items on the line, and it's not the header line.
                            // 14 is enough to know the action, 32 is enough to have details for a shot.
                            if(strArr.length >=14 && lineCount > 1)
                            {
                                // Get the stuff we care about
                                int period = Integer.parseInt(strArr[10]);
                                String time = strArr[11];
                                String team = strArr[12];
                                String action = strArr[13];
                                String player = "";
                                String result = "";
                                String type = "";
                                int x = -1;
                                int y = -1;
                                if(strArr.length == 32)
                                {
                                    player = strArr[23];
                                    result = strArr[27];
                                    type = strArr[29];
                                    x = Integer.parseInt(strArr[30]);
                                    y = Integer.parseInt(strArr[31]);
                                }

//                                System.out.println(action);

                                // I want to convert time to an integer representing number of seconds elapsed in the game
                                //  so that's what I do here.

                                int seconds_elapsed_in_game = (period - 1) * 12*60;

                                int minutes = Integer.parseInt(time.split(":")[0]);
                                int seconds = Integer.parseInt(time.split(":")[1]);

                                seconds_elapsed_in_game += (11 - minutes)*60;
                                seconds_elapsed_in_game += (60 - seconds);

                                if(period != prev_period)
                                {
                                    last_reset_value = 24;
                                    last_time_of_reset = (period-1) * 12 * 60;
                                }

                                // Shot clock calculation
                                //  There are a few problems here, but it should give us a bit more detail.
                                //  Specifically, when a shot misses, we need to assume it hit the basket, otherwise
                                //  the clock would not reset.  However, we don't have that information in the dataset
                                //  so this may be a faulty assumption.
                                int shot_clock = last_reset_value - (seconds_elapsed_in_game - last_time_of_reset);
                                // One other thing to note.  Shot clock calculation is iffy because there is a
                                //  variable amount of time between when a ball is inbounded (i.e. after a shot)
                                //  and the shot clock resets and starts.  As such, I allow some slightly negative shot
                                //  clock readings.  Since this is going into a euclidean distance calculation, I don't
                                //  think this will be a huge factor, however, it should be noted that the shot clock
                                //  value contained in a shot cannot be trusted for accurate shot clock information.
                                if(shot_clock > 24)
                                {
                                    System.out.println("Bad shot clock reading");
                                }

                                // Shots are what we're most interested in, so this will be the biggest clause
                                if(action.equals("shot"))
                                {

                                    Shot this_shot = new Shot();
                                    this_shot._shooter = player;
                                    this_shot._shot_made = result.equals("made");
                                    this_shot._shot_type = type;
                                    this_shot._seconds_elapsed_in_game = seconds_elapsed_in_game;
                                    this_shot._time_on_shot_clock = shot_clock;
                                    this_shot._x = x;
                                    this_shot._y = y;

                                    all_shots.get(this_date).add(this_shot);
                                }

                                // Now we need to figure out whether to reset the shot clock:
                                // Actions that reset the clock to 24:
                                //  shot (assumes misses are bricks)
                                //  turnover
                                //  free throw
                                //  defensive rebound
                                //  change of period
                                // Actions that reset the clock to 14:
                                //  defensive foul not resulting in free throws, with shot clock below 13
                                // Actions that reset the clock to 5:
                                //  offensive recovery of jump ball and shot clock below 5
                                //  offensive timeout

                                // This code can be compacted, but I'm opting for readability
                                if(action.equals("shot"))
                                {
                                    last_reset_value = 24;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("free throw"))
                                {
                                    last_reset_value = 24;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("turnover"))
                                {
                                    last_reset_value = 24;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("rebound") && !last_possessed_by.equals(team))
                                {
                                    last_reset_value = 24;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("foul") && !last_possessed_by.equals(team) && shot_clock <= 13)
                                {
                                    last_reset_value = 14;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("jump ball") && last_possessed_by.equals(team) && shot_clock < 5)
                                {
                                    last_reset_value = 5;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }
                                if(action.equals("timeout") && shot_clock < 5)
                                {
                                    last_reset_value = 5;
                                    last_time_of_reset = seconds_elapsed_in_game;
                                }

                                // One last thing.  Need to set the shot clock equal to the seconds left in the
                                //  period if there are fewer than last_reset_value seconds left in the period.
                                //
                                //  Bit of detail here.
                                //      12*60 is the length of a period.
                                //      seconds_elapsed_in_game % 12*60 is the seconds elapsed in this period
                                //      12*60 - the previous line is the seconds left in the period
                                //      if the previous line is less than the last reset value, reset the clock to that instead
                                if(12*60 - (seconds_elapsed_in_game % (12*60)) < last_reset_value )
                                {
                                    last_reset_value = 12*60 - (seconds_elapsed_in_game % (12*60));
                                }

                                last_possessed_by = team;
                                prev_period = period;
                            }
                        }
                    }
                }
            }
        }

        return all_shots;
    }
}

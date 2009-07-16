package turk;

import haus.io.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import evaluation.KappaIAA;


/**
 * Designed to preform all sorts of analysis on the the Turk data from the task of 
 * labeling causes and effects in a sentence.
 * 
 * It also is used to output our CRF format in either Human Readable or 
 * CRF Readable form.
 *
 */
public class TokenTaskAnalyzer {
	public static final String HIT_KEY = "HITId";
	public static final String WORKER_KEY = "WorkerId";
	public static final String OPTION_BOX_KEY = "Answer.Causal_Option_Box";
	public static final String RADIO_BOX_KEY = "Answer.table";
	public static final String SENTENCE_KEY = "Input.sentence";
	
	public static final String YES_ANS = "Yes";
	public static final String NO_ANS = "No";
	public static final String UNSURE_ANS = "Unsure";
	public static final String CAUSE_ANS = "Cause";
	public static final String EFFECT_ANS = "Effect";
	public static final String NEITHER_ANS = "Neither";
		
	TurkReader treader = null;
	String inFile = "turk/Batch_Files/causeEffectResults.csv";
	//String outFile = "turk/causeEffectAnalysis.csv";
	String approveRejectFile = "turk/approveReject.csv";
	String readableTurkAnalysisFile = "turk/readableAnalysis.csv";
	String crfOutFile = "turk/crfOut.txt";
	String negExamples = "turk/Non-CausalSentences.txt";
	
	public TokenTaskAnalyzer () {
		treader = new TurkReader();
		treader.parseBatchFile(inFile);
	}
	
	/**
	 * Generic questionMap meant for dealing with common traversals
	 * encountered by this class
	 */
	abstract class questionMap {
		/**
		 * How to handle the reponses to each HIT
		 */
		abstract void handleResponseList (String hitID, ArrayList<String[]> responseList);
		
		/**
		 * How to handle the responses to a given row
		 */
		void handleRowResponse (String hitID, String[] response) {};
	}
	
	/**
	 * Checks a given response for good quality
	 * @param responses
	 */
	@SuppressWarnings("unchecked")
	class qualityCheckerMap extends questionMap {
		Hashtable<String, Boolean> blacklist = new Hashtable<String,Boolean>();
		//Hashtable<String, Boolean> blacklistedIDS = (Hashtable<String, Boolean>) haus.io.Serializer.deserialize("turk/blacklistedIDS.ser");
		Hashtable<String, Boolean> blacklistedIDS = new Hashtable<String,Boolean>();
		int uniqueworkers = 0, blacklistedworkers = 0;
		
		void handleResponseList (String hitID, ArrayList<String[]> responseList) {
			rowMajorTraversal(hitID, responseList, this);
		};
		
		/**
		 * Looks at a single row and checks the answer quality
		 */
		void handleRowResponse (String hitID, String[] response) {
			String workerID = getWorkerID(response);
			String answer = getCausalOptionBox(response);
			String[] boxes = decodeOptionBox(answer);
			if (!blacklistedIDS.containsKey(workerID)) {
				blacklistedIDS.put(workerID, false);
				uniqueworkers++;
			}
			if (answer.indexOf("Yes") == -1) {
				blacklist.put(hash(hitID,workerID), true);
				if (!blacklistedIDS.get(workerID)) {
					blacklistedIDS.put(workerID, true);
					blacklistedworkers++;
				}
			}

			// Check to make sure each Yes answer has at least 1 Cause and Effect
			int numWrong = 0;
			for (int sentenceNum = 0; sentenceNum < 10; sentenceNum++) {
				if (boxes[sentenceNum].equals("No") || boxes[sentenceNum].equals("Unsure"))
					continue;
				boolean hasCause = false; boolean hasEffect = false;
				String sent = getSentence(response, sentenceNum);
				String[] segs = sent.split(" ");
				for (int wordNum = 0; wordNum < segs.length; wordNum++) {
					String radioAns = getRadioResponse(response, sentenceNum, wordNum);
					if (radioAns == null)
						continue;
					if (radioAns.equals("Cause"))
						hasCause = true;
					else if (radioAns.equals("Effect"))
						hasEffect = true;
				}
				if (!hasCause || !hasEffect)
					numWrong++;
			}
			if (numWrong >= 1) {
				//System.out.println("UID: " + workerID + " QID: " + hitID + " NumWrong: " + 
				//		numWrong + " -Didn't select cause and effect");
				blacklist.put(hash(hitID,workerID), true);
				if (!blacklistedIDS.get(workerID)) {
					blacklistedIDS.put(workerID, true);
					blacklistedworkers++;
				}
			}
		}
		
		/**
		 * Check the blacklist
		 */
		boolean blacklisted (String hitID, String workerID) {
			return blacklist.containsKey(hash(hitID,workerID));
		}
		
		/**
		 * Check if a worker is blacklisted.
		 * @param wokerID
		 * @return
		 */
		boolean blacklisted (String wokerID) {
			return blacklistedIDS.get(wokerID);
		}
		
		/**
		 * Writes a new file with x's in the approve column for all good hits
		 * and x's in the reject column for the bad hits
		 */
		void approveReject (String reviewFile) {
			DataWriter writer = new DataWriter(reviewFile);
			TurkReader.TurkAssistedReader areader = treader.new TurkAssistedReader(inFile);
			
			int rejected = 0; int approved = 0;
			String[] segs;
			while ((segs = areader.getNextLine()) != null) {
				String tuple = areader.fullLine;
				String hitID = treader.decode("HITId", segs);
				String workerID = treader.decode("WorkerId", segs);
				if (blacklist.containsKey(hash(hitID,workerID))) {
					tuple += ",\"\",\"x\""; // Reject Hit
					rejected++;
				} else {
					tuple += ",\"x\",\"\""; // Approve Hit
					approved++;
				}
				writer.write(tuple + "\n");
				tuple = "";
			}
			writer.close();
			System.out.printf("%d were blacklisted %d were disqualified %d were approved\n", 
					blacklist.size(), rejected, approved);
		}
		
		String hash (String hitID, String workerID) {
			return hitID+"_"+workerID;
		}
	}
	
	/**
	 * Designed to output human readable file of the form
	 * HitID# Sent# Person# CausalOptionBox Sentence
	 */
	class humanFormatterMap extends questionMap {
		ArrayList<String> newTuples = null;
		String header = "\"HitID\",\"Sentence #\",\"WorkerID\",\"Label\",\"Sentence\"";
		DataWriter writer = new DataWriter(readableTurkAnalysisFile);
		
		/**
		 * Basically a rowMajorTraversal with a bit of re-arrangement
		 */
		void handleResponseList(String hitID, ArrayList<String[]> responseList) {
			newTuples = new ArrayList<String>();
			rowMajorTraversal(hitID, responseList, this);
			for (int i = 0; i < 10; i++) {
				for (int j = 0; j < 5; j++) {
					writer.write(newTuples.get(10 * j + i) + "\n");
				}
				writer.write("\n");
			}
		}
		
		/**
		 * Main job is to format [causes]C and [effects]E
		 */
		void handleRowResponse (String hitID, String[] response) {
			String workerID = getWorkerID(response);
			String[] boxes = decodeOptionBox(getCausalOptionBox(response));

			for (int sentenceNum = 0; sentenceNum < 10; sentenceNum++) {
				String sentence = getSentence(response, sentenceNum);
				String[] words = sentence.split(" ");	
				String[] segs = new String[5];
				segs[0] = hitID;
				segs[1] = Integer.toString(sentenceNum);
				segs[2] = workerID;
				segs[3] = boxes[sentenceNum];
				segs[4] = sentence;
				
				if (boxes[sentenceNum].equals("No") || boxes[sentenceNum].equals("Unsure")) {
					newTuples.add(Include.makeCSV(segs));
					continue;
				}
				
				String line = "";
				boolean inCause = false; boolean inEffect = false;
				for (int wordNum = 0; wordNum < words.length; wordNum++) {
					String word = words[wordNum];
					String radioAns = getRadioResponse(response, sentenceNum, wordNum);
					String nextAns = wordNum == words.length-1 ? "" : 
						getRadioResponse(response, sentenceNum, wordNum+1);
					
					if (radioAns.equals(CAUSE_ANS)) {
						if (nextAns.equals(CAUSE_ANS)) {
							if (inCause)
								line += word;
							else
								line += "[" + word;
						} else {
							if (inCause)
								line += word + "]C";
							else
								line += "[" + word + "]C";
						}
					} else if (radioAns.equals(EFFECT_ANS)) {
						if (nextAns.equals(EFFECT_ANS)) {
							if (inEffect)
								line += word;
							else
								line += "[" + word;
						} else {
							if (inEffect)
								line += word + "]E";
							else
								line += "[" + word + "]E";
						}
					} else if (radioAns.equals(NEITHER_ANS))
						line += word;
					line += " ";
					
					if (radioAns.equals(CAUSE_ANS)) {
						inCause = true;
						inEffect = false;
					} else if(radioAns.equals(EFFECT_ANS)) {
						inEffect = true;
						inCause = false;
					} else 
						inEffect = inCause = false;				
				}
				
				segs[4] = line;
				newTuples.add(Include.makeCSV(segs));
			}
		}
		
		void writeHeader () {
			writer.write(header + "\n");
		}
		
		void close () {
			writer.close();
		}
	}
	
	/**
	 * Intended to format the Turk batch in the desired
	 * manner to serve as input for the CRF 
	 */
	class crfFormatterMap extends questionMap {
		public boolean infoGathered = false;
		userRater rater = new userRater();
		qualityCheckerMap qualChecker = new qualityCheckerMap();
		crfFileWriter writer = new crfFileWriter();
		
		class crfFileWriter {
			DataWriter writer = new DataWriter(crfOutFile);
			String ancientResponse = "";
			String lastResponse = " ";
			String lastWord = "";
			
			public void write (String word, String response) {
				if (response.equals(lastResponse)) {
					if (lastResponse.equals(ancientResponse))
						writeFullCorres();
					else
						writeNewCorres();
				} else {
					if (lastResponse.equals(ancientResponse))
						writeOldCorres();
					else
						writeNoCorres();
				}
				ancientResponse = lastResponse;
				lastResponse = response;
				lastWord = word;
			}

			void writeFullCorres () {
				if (lastResponse.equals(CAUSE_ANS))
					writeln(mallet.Include.CAUSE_INTERMEDIATE_TAG);
				else if (lastResponse.equals(EFFECT_ANS))
					writeln(mallet.Include.EFFECT_INTERMEDIATE_TAG);
				else if (lastResponse.equals(NEITHER_ANS))
					writeln(mallet.Include.NEITHER_TAG);
			}
			
			void writeNewCorres () {
				if (lastResponse.equals(CAUSE_ANS))
					writeln(mallet.Include.CAUSE_BEGIN_TAG);
				else if (lastResponse.equals(EFFECT_ANS))
					writeln(mallet.Include.EFFECT_BEGIN_TAG);
				else if (lastResponse.equals(NEITHER_ANS))
					writeln(mallet.Include.NEITHER_TAG);
			}
			
			void writeOldCorres () {
				if (lastResponse.equals(CAUSE_ANS))
					writeln(mallet.Include.CAUSE_END_TAG);
				else if (lastResponse.equals(EFFECT_ANS))
					writeln(mallet.Include.EFFECT_END_TAG);
				else if (lastResponse.equals(NEITHER_ANS))
					writeln(mallet.Include.NEITHER_TAG);
			}
			
			void writeNoCorres () {
				if (lastResponse.equals(CAUSE_ANS))
					writeln(mallet.Include.CAUSE_TAG);
				else if (lastResponse.equals(EFFECT_ANS))
					writeln(mallet.Include.EFFECT_TAG);
				else if (lastResponse.equals(NEITHER_ANS))
					writeln(mallet.Include.NEITHER_TAG);
			}
			
			void writeln (String answer) {
				writer.write(lastWord + " " + answer + "\n");
			}
			
			void flush () {
				write("","");
				writer.close();
			}
		}
		
		public crfFormatterMap () {
			traverseAnswers(rater);
			traverseAnswers(qualChecker);
		}
		
		void close () {
			writer.flush();
		}
		
		void handleResponseList(String hitID, ArrayList<String[]> responseList) {
			evalTokenMajorities(hitID, responseList);
		}
		
		/**
		 * Gets the majority consensus for each token 
		 */
		void evalTokenMajorities (String hitID, ArrayList<String[]> responseList) {
			String[] workerIDS = getColMajorResponse(WORKER_KEY,responseList);
			String[] causalAnswers = getColMajorResponse(OPTION_BOX_KEY,responseList);
			
			for (int sentNum = 0; sentNum < Include.SENT_PER_HIT; sentNum++) {
				String sentence = getSentence(responseList.get(0),sentNum); //Should be same
				String[] words = sentence.split(" ");
				ArrayList<String> yesWorkers = new ArrayList<String>();
				ArrayList<String> noWorkers = new ArrayList<String>();
				ArrayList<String> unsureWorkers = new ArrayList<String>();
				int yes = 0; int no = 0; int unsure = 0;
				for (int workerNum = 0; workerNum < workerIDS.length; workerNum++) {
					String workerID = workerIDS[workerNum];
					//if (qualChecker.blacklisted(hitID, workerID))
					if (qualChecker.blacklisted(workerID))
						continue;
					String ans = decodeOptionBox(causalAnswers[workerNum])[sentNum];
					if (ans.equals(YES_ANS)) {
						yes++;
						yesWorkers.add(workerID);
					} else if (ans.equals(NO_ANS)) {
						no++;
						noWorkers.add(workerID);
					} else if (ans.equals(UNSURE_ANS)) {
						unsure++;
						unsureWorkers.add(workerID);
					}
				}
				String majAns = getMajority(yes,no,unsure,YES_ANS,NO_ANS,UNSURE_ANS,yesWorkers, noWorkers, unsureWorkers);
				if (!majAns.equals(YES_ANS)) {
					for (int i = 0; i < words.length; i++) {
						writer.write(words[i],NEITHER_ANS);
					}
					writer.write(mallet.Include.SENT_DELIM_REDUX, NEITHER_ANS);
					continue;
				}
				
				for (int wordNum = 0; wordNum < words.length; wordNum++) {
					int cause = 0; int effect = 0; int neither = 0;
					ArrayList<String> causeWorkers = new ArrayList<String>();
					ArrayList<String> effectWorkers = new ArrayList<String>();
					ArrayList<String> neitherWorkers = new ArrayList<String>();
					
					for (int workerNum = 0; workerNum < workerIDS.length; workerNum++) {
						String workerID = workerIDS[workerNum];
						//if (qualChecker.blacklisted(hitID, workerID))
						if (qualChecker.blacklisted(workerID))
							continue;
						if (decodeOptionBox(causalAnswers[workerNum])[sentNum].equals(NO_ANS))
							continue;
						String ans = getRadioResponse(responseList.get(workerNum), sentNum, wordNum);
						if (ans == null) {
							continue;
						}
						if (ans.equals(CAUSE_ANS)) {
							cause++;
							causeWorkers.add(workerID);
						} else if (ans.equals(EFFECT_ANS)) {
							effect++;
							effectWorkers.add(workerID);
						} else if (ans.equals(NEITHER_ANS)) {
							neither++;
							neitherWorkers.add(workerID);
						}
					}
					//String majorityAnswer = getMajority(cause,effect,neither, CAUSE_ANS, EFFECT_ANS, NEITHER_ANS,
					//		causeWorkers, effectWorkers, neitherWorkers);
					String majorityAnswer = getMajority(CAUSE_ANS,EFFECT_ANS,NEITHER_ANS, causeWorkers,
							effectWorkers, neitherWorkers);
					writer.write(words[wordNum],majorityAnswer);
				}
				writer.write(mallet.Include.SENT_DELIM_REDUX, NEITHER_ANS);
			}
		}
		
		/**
		 * Establishes a majority for a token label. This is done based on the ratings of the
		 * users which picked each different label (only if there is a  tie).
		 */
		String getMajority (int cause, int effect, int neither, String causeTag, String effectTag, String neitherTag,
				ArrayList<String> causeWorkers, ArrayList<String> effectWorkers, ArrayList<String> neitherWorkers) {
			String majority = "";
			int largest;
			ArrayList<String> majArray;
			if (cause >= effect) {
				majority = causeTag;
				largest = cause;
				majArray = causeWorkers;
			} else {
				majority = effectTag;
				largest = effect;
				majArray = effectWorkers;
			}
			if (cause == effect) {
				if (betterRated(causeWorkers,effectWorkers)) {
					majority = causeTag;
					largest = cause;
					majArray = causeWorkers;
				} else {
					majority = effectTag;
					largest = effect;
					majArray = effectWorkers;
				}
			}
			
			if (neither > largest) {
				majority = neitherTag;
			} else if (neither == largest) {
				if (betterRated(neitherWorkers, majArray))
					majority = neitherTag;
			}
			return majority;
		}
		
		/**
		 * Returns the majority consensus based on user ratings.
		 * @param causeWord
		 * @param effectWord
		 * @param neitherWord
		 * @param causeWorkers
		 * @param effectWorkers
		 * @param neitherWorkers
		 * @return
		 */
		String getMajority (String causeWord, String effectWord, String neitherWord, 
				ArrayList<String> causeWorkers, ArrayList<String> effectWorkers, 
				ArrayList<String> neitherWorkers) {
			int causeTotal = repTotal(causeWorkers);
			int effectTotal = repTotal(effectWorkers);
			int neitherTotal = repTotal(neitherWorkers);
			
			String majority = "";
			int largest;
			if (causeTotal >= effectTotal) {
				majority = causeWord;
				largest = causeTotal;
			} else {
				majority = effectWord;
				largest = effectTotal;
			}
			
			if (neitherTotal > largest) {
				majority = neitherWord;
			}
			return majority;
		}
		
		int repTotal (ArrayList<String> workers) {
			int total = 0;
			for (String wid : workers)
				total += rater.getUserRating(wid);
			return total;
		}
		
		/**
		 * Returns true if the workers from the first arraylist have 
		 * beter ratings than those from the second
		 */
		boolean betterRated (ArrayList<String> firstList, ArrayList<String> secondList) {
			int fTotal = 0;
			int lTotal = 0;
			for (int i = 0; i < firstList.size(); i++) {
				String workerID = firstList.get(i);
				fTotal += rater.getUserRating(workerID);
			}
			for (int i = 0; i < secondList.size(); i++) {
				String workerID = secondList.get(i);
				lTotal += rater.getUserRating(workerID);
			}
			double av1 = fTotal / (double) firstList.size();
			double av2 = lTotal / (double) secondList.size();
			if (av1 >= av2)
				return true;
			return false;
		}
		
		/**
		 * Will write the negative or non-causal examples which users 
		 * have elminitated by the 1st Turk task CRF needs to use these as well
		 */
		void writeNegatives () {
			FileReader reader = new FileReader(negExamples);
			String line;
			while ((line = reader.getNextLine()) != null) {
				String[] words = line.split(" ");
				for (int i = 0; i < words.length; i++) { 
					writer.write(words[i], NEITHER_ANS);
				}
				writer.write(mallet.Include.SENT_DELIM_REDUX, NEITHER_ANS);
			}
		}
	}
	
	/**
	 * Gets a rating for every user based on how well that user 
	 * selected sentences as causal/non-causal 
	 */
	class userRater extends questionMap {
		Hashtable<String,userProfiler> userRatings = new Hashtable<String,userProfiler>();
		
		/**
		 * Keeps track of a single user
		 */
		class userProfiler {
			String workerID;
			int correct = 0;
			int wrong = 0;
			public userProfiler (String _workerID) {
				workerID = _workerID;
			}
			public void correct () {
				correct++;
			}
			public void wrong () {
				wrong++;
			}
			public int getScore () {
				return correct - wrong;
			}
			public double getPercScore () {
				return (correct)/(double)(correct+wrong);
			}
		}
		
		Hashtable<String,Integer> getUserRatings () {
			Hashtable<String,Integer> out = new Hashtable<String,Integer>();
			Enumeration<String> e = userRatings.keys();
			while (e.hasMoreElements()) {
				String elem = e.nextElement();
				out.put(elem, userRatings.get(elem).getScore());
			}
			return out;
		}
		
		/**
		 * Returns the user's rating.
		 */
		Integer getUserRating (String uid) {
			if (userRatings.containsKey(uid))
				return userRatings.get(uid).getScore();
			return null;
		}
		
		Double getUserRatingPerc (String uid) {
			if (userRatings.containsKey(uid))
				return userRatings.get(uid).getPercScore();
			return null;
		}

		void handleResponseList(String hitID, ArrayList<String[]> responseList) {
			getUserRatings(responseList);
		}
		
		/**
		 * Gives each user a rating based on whether or not they agreed with the 
		 * consensus vote on causal/non-causal for sentences
		 */
		void getUserRatings (ArrayList<String[]> responseList) {
			String[] workerIDS = getColMajorResponse(WORKER_KEY,responseList);
			String[] causalAnswers = getColMajorResponse(OPTION_BOX_KEY,responseList);
			
			
			for (int sentNum = 0; sentNum < Include.SENT_PER_HIT; sentNum++) {
				int yes = 0; int no = 0; int unsure = 0;
				for (int workerNum = 0; workerNum < workerIDS.length; workerNum++) {
					String ans = decodeOptionBox(causalAnswers[workerNum])[sentNum];
					if (ans.equals(YES_ANS))
						yes++;
					if (ans.equals(NO_ANS))
						no++;
					if (ans.equals(UNSURE_ANS))
						unsure++;
				}
				String majorityAnswer = getMajority(yes,no,unsure);
				for (int workerNum = 0; workerNum < workerIDS.length; workerNum++) {
					String workerID = workerIDS[workerNum];
					if (!userRatings.containsKey(workerID))
						userRatings.put(workerID, new userProfiler(workerID));
					String ans = decodeOptionBox(causalAnswers[workerNum])[sentNum];
					if (ans.equals(majorityAnswer))
						userRatings.get(workerID).correct();
					else
						userRatings.get(workerID).wrong();
				}
			}
		}
		
		/**
		 * Determines Majority Answer on Causal Option box YES|NO|UNSURE
		 * Ties are split in this priority: Yes, No, Unsure
		 */
		String getMajority (int numYes, int numNo, int numUnsure) {
			String majority = "";
			int largest;
			if (numYes >= numNo) {
				majority = YES_ANS;
				largest = numYes;
			} else {
				majority = NO_ANS;
				largest = numNo;
			}
			
			if (numUnsure > largest) {
				majority = UNSURE_ANS;
			}
			return majority;
		}
	}
	
	class agreementFinder extends questionMap {
		KappaIAA kappa = new KappaIAA();
		qualityCheckerMap qualChecker = new qualityCheckerMap();
		
		public agreementFinder () {
			traverseAnswers(qualChecker);
		}
		
		void handleResponseList(String hitID, ArrayList<String[]> responseList) {
			//getSentLevelAgreement(responseList);
			getTokLevelAgreement(responseList);
		}
		
		void getSentLevelAgreement (ArrayList<String[]> responseList) {
			String[] workerIDS = getColMajorResponse(WORKER_KEY,responseList);
			String[] causalAnswers = getColMajorResponse(OPTION_BOX_KEY,responseList);
			
			ArrayList<String> ids = new ArrayList<String>();
			ArrayList<String> ansids = new ArrayList<String>();
			for (int i = 0; i < workerIDS.length; i++) {
				String id = workerIDS[i];
				if (!qualChecker.blacklisted(id)) {
					ids.add(id);
					ansids.add(causalAnswers[i]);
				}
			}
			workerIDS = haus.misc.Conversions.toStrArray(ids);
			causalAnswers = haus.misc.Conversions.toStrArray(ansids);
			
			for (int i = 0; i < Include.SENT_PER_HIT; i++) {
				
				boolean agree = true;
				int unsure = 0;
				String startAns = causalAnswers[0].split("\\|")[i];
				for (String answer : causalAnswers) {
					String oans = answer.split("\\|")[i];
					if (oans.equals(UNSURE_ANS)) {
						unsure++;
						continue;
					}
					if (!answer.split("\\|")[i].equals(startAns))
						agree = false;
				}
				double expectation = workerIDS.length == 1 + unsure ? 1.0 : 1 / Math.pow(2, (workerIDS.length - (1 + unsure)));
				if (workerIDS.length == 1 || workerIDS.length == 1 + unsure)
					return;
				kappa.addDataPoint();
				kappa.addExpected(expectation);
				if (agree)
					kappa.addAgree();
			}
		}
		
		void getTokLevelAgreement (ArrayList<String[]> responseList) {
			String[] workerIDS = getColMajorResponse(WORKER_KEY,responseList);
			String[] causalAnswers = getColMajorResponse(OPTION_BOX_KEY,responseList);
			
			// Remove blacklisted ppl
			ArrayList<String> ids = new ArrayList<String>();
			ArrayList<String> ansids = new ArrayList<String>();
			for (int i = 0; i < workerIDS.length; i++) {
				String id = workerIDS[i];
				if (!qualChecker.blacklisted(id)) {
					ids.add(id);
					ansids.add(causalAnswers[i]);
				}
			}
			workerIDS = haus.misc.Conversions.toStrArray(ids);
			causalAnswers = haus.misc.Conversions.toStrArray(ansids);
			
			for (int i = 0; i < Include.SENT_PER_HIT; i++) {
				String sentence = getSentence(responseList.get(0),i);
				int sentLen = sentence.split(" ").length;
				ArrayList<String[]> annotations = new ArrayList<String[]>();
				for (int j = 0; j < workerIDS.length; j++) {
					String causal = decodeOptionBox(causalAnswers[j])[i];
					if (causal.equals(YES_ANS)) {
						String[] radioAnswers = new String[sentLen];
						boolean hasCP=false,hasEP=false;
						for (int k = 0; k < sentLen; k++) {
							String radioAnswer = getRadioResponse(responseList.get(j),i,k);
							radioAnswers[k] = radioAnswer;
							if (radioAnswer.equals(CAUSE_ANS))
								hasCP = true;
							if (radioAnswer.equals(EFFECT_ANS))
								hasEP = true;
						}
						if (hasEP && hasCP)
							annotations.add(radioAnswers);
					}
				}
				if (annotations.size() > 1)
					getTokenAgreement(annotations);
			}
		}
		
		void getTokenAgreement (ArrayList<String[]> annotations) {
			int len = annotations.get(0).length;
			for (int i = 0; i < len; i++) {
				boolean matching = true;
				String start = annotations.get(0)[i];
				for (int j = 1; j < annotations.size(); j++) {
					String other = annotations.get(j)[i];
					if (!other.equals(start))
						matching = false;
				}
				kappa.addDataPoint();
				if (matching) kappa.addAgree();
				double expectation = 1 / Math.pow(3, (annotations.size()-1));
				kappa.addExpected(expectation);
			}
		}
		
		void print () {
			kappa.printKappa();
		}
	}
	
	/**
	 * Gets the workerID from a tuple
	 */
	String getWorkerID (String[] response) {
		return treader.decode(WORKER_KEY, response);
	}
	
	/**
	 * Gets the Causal Option Box Response from a tuple
	 */
	String getCausalOptionBox (String[] response) {
		return treader.decode(OPTION_BOX_KEY, response);
	}
	
	/**
	 * Given a causal option box of the form 
	 * Yes|No|Yes|Unsure. This will decode.
	 */
	String[] decodeOptionBox (String options) {
		return options.split("\\|");
	}
	
	/**
	 * Gets the response of a radio button from a tuple
	 */
	String getRadioResponse (String[] response, int sentNum, int wordNum) {
		String query = RADIO_BOX_KEY + sentNum + "Q" + wordNum;
		return treader.decode(query, response);
	}
	
	/**
	 * Gets a given Sentence from a tuple
	 */
	String getSentence (String[] response, int sentNum) {
		String query = SENTENCE_KEY + sentNum;
		return treader.decode(query, response);
	}
	
	/**
	 * Traverses each of the Turk's answers - calling 
	 * the map
	 */
	public void traverseAnswers (questionMap map) {
		String[] questions = treader.getClusterKeys();
		for (String hitID : questions) {
			ArrayList<String[]> responses = treader.getClusterResponses(hitID);
			map.handleResponseList(hitID, responses);
		}
	}
	
	/**
	 * Traverses each response row individually
	 */
	public void rowMajorTraversal (String hitID, ArrayList<String[]> responses, questionMap map) {
		for (String[] response : responses)
			map.handleRowResponse(hitID, response);
	}
	
	/**
	 * Returns an array of responses to the given column name
	 */
	public String[] getColMajorResponse (String colName, ArrayList<String[]> responses) {
		String[] columnResponses = new String[responses.size()];
		for (int responseNum = 0; responseNum < responses.size(); responseNum++) {
			String[] response = responses.get(responseNum);
			columnResponses[responseNum] = treader.decode(colName, response);
		}
		return columnResponses;
	}
	
	/**
	 * Checks the quality of each Turk's answers, disqualifying
	 * those who have obviously not done work.
	 */
	public void checkQuality () {
		qualityCheckerMap map = new qualityCheckerMap();
		traverseAnswers(map);
		System.out.println("Total: " + map.uniqueworkers + " blacklisted: " + map.blacklistedworkers);
		//map.approveReject(approveRejectFile);
	}
	
	public void getUserRatings () {
		userRater rater = new userRater();
		traverseAnswers(rater);
	}
	
	public void getAgreement () {
		agreementFinder agg = new agreementFinder();
		traverseAnswers(agg);
		agg.print();
	}
	
	/**
	 * Outputs a human readable version of the Turk batch
	 */
	public void makeReadable () {
		humanFormatterMap map = new humanFormatterMap();
		map.writeHeader();
		traverseAnswers(map);
		map.close();
	}
	
	public void doCRFFormat () {
		crfFormatterMap map = new crfFormatterMap();
		traverseAnswers(map);
		//map.writeNegatives();
		map.close();
	}
	
	public void interactiveQuery () {
		userRater map = new userRater ();
		traverseAnswers(map);
		System.out.println("Enter new UID:");
		String CurLine = ""; // Line read from standard in
		InputStreamReader converter = new InputStreamReader(System.in);
		BufferedReader in = new BufferedReader(converter);
		while (!(CurLine.equals("quit"))){
			try {
				CurLine = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (!(CurLine.equals("quit"))){
				System.out.print(map.getUserRating(CurLine) + " Perc:" + map.getUserRatingPerc(CurLine) + "\n>");
			}
		}
	}
	
	public static void main (String[] args) {
		TokenTaskAnalyzer tta = new TokenTaskAnalyzer();
		//tta.doCRFFormat();
		//tta.checkQuality();
		//tta.interactiveQuery();
		//tta.getUserRatings();
		tta.getAgreement();
	}
}

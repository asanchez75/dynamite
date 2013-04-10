package nl.vu.cs.querypie.reasoner.actions.io;

import java.util.Arrays;

import nl.vu.cs.ajira.actions.Action;
import nl.vu.cs.ajira.actions.ActionConf;
import nl.vu.cs.ajira.actions.ActionContext;
import nl.vu.cs.ajira.actions.ActionFactory;
import nl.vu.cs.ajira.actions.ActionOutput;
import nl.vu.cs.ajira.actions.ActionSequence;
import nl.vu.cs.ajira.data.types.TInt;
import nl.vu.cs.ajira.data.types.TLong;
import nl.vu.cs.ajira.data.types.Tuple;
import nl.vu.cs.ajira.exceptions.ActionNotConfiguredException;
import nl.vu.cs.ajira.utils.Utils;
import nl.vu.cs.querypie.ReasoningContext;
import nl.vu.cs.querypie.reasoner.support.ParamHandler;
import nl.vu.cs.querypie.storage.BTreeInterface;
import nl.vu.cs.querypie.storage.DBType;
import nl.vu.cs.querypie.storage.WritingSession;

public class WriteDerivationsBtree extends Action {
	public static void addToChain(ActionSequence actions) throws ActionNotConfiguredException {
		ActionConf c = ActionFactory.getActionConf(WriteDerivationsBtree.class);
		actions.add(c);
	}

	private BTreeInterface in;
	private WritingSession spo, sop, pos, pso, osp, ops;
	private boolean newValue;
	private final byte[] triple = new byte[24];
	byte[] toWrite;
	private final byte[] meta = new byte[8];
	private long dupCount, newCount;

	@Override
	public void startProcess(ActionContext context) throws Exception {
		in = (BTreeInterface) ReasoningContext.getInstance().getKB();
		spo = in.openWritingSession(DBType.SPO);
		sop = in.openWritingSession(DBType.SOP);
		pso = in.openWritingSession(DBType.PSO);
		pos = in.openWritingSession(DBType.POS);
		osp = in.openWritingSession(DBType.OSP);
		ops = in.openWritingSession(DBType.OPS);
		newValue = false;
		newCount = dupCount = 0;
	}
	
	private byte[] encode(long l1, long l2, long l3) {
		int sz = in.encode(triple, l1, l2, l3);
		if (sz < triple.length) {
			return Arrays.copyOf(triple, sz);
		}
		return triple;
	}

	@Override
	public void process(Tuple tuple, ActionContext context, ActionOutput actionOutput) throws Exception {
		TLong s = (TLong) tuple.get(0);
		TLong p = (TLong) tuple.get(1);
		TLong o = (TLong) tuple.get(2);
		TInt step = (TInt) tuple.get(3);

		byte[] toWrite = encode(s.getValue(), p.getValue(), o.getValue());
		Utils.encodeInt(meta, 0, step.getValue());

		boolean newTuple = false;
		if (ParamHandler.get().isUsingCount()) {
			TInt count = (TInt) tuple.get(4);
			int c = count.getValue();
			newTuple = spo.writeWithCount(toWrite, meta, c, false) == WritingSession.SUCCESS;

			// Add it also in the other permutations
			toWrite = encode(s.getValue(), o.getValue(), p.getValue());
			sop.writeWithCount(toWrite, meta, c, newTuple);

			toWrite = encode(p.getValue(), o.getValue(), s.getValue());
			pos.writeWithCount(toWrite, meta, c, newTuple);

			toWrite = encode(p.getValue(), s.getValue(), o.getValue());
			pso.writeWithCount(toWrite, meta, c, newTuple);

			toWrite = encode(o.getValue(), p.getValue(), s.getValue());
			ops.writeWithCount(toWrite, meta, c, newTuple);

			toWrite = encode(o.getValue(), s.getValue(), p.getValue());
			osp.writeWithCount(toWrite, meta, c, newTuple);
		} else {
			newTuple = spo.write(toWrite, meta) == WritingSession.SUCCESS;

			// Add it also in the other permutations
			toWrite = encode(s.getValue(), o.getValue(), p.getValue());
			sop.write(toWrite, meta);

			toWrite = encode(p.getValue(), o.getValue(), s.getValue());
			pos.write(toWrite, meta);

			toWrite = encode(p.getValue(), s.getValue(), o.getValue());
			pso.write(toWrite, meta);

			toWrite = encode(o.getValue(), p.getValue(), s.getValue());
			ops.write(toWrite, meta);

			toWrite = encode(o.getValue(), s.getValue(), p.getValue());
			osp.write(toWrite, meta);
		}

		if (newTuple) {
			if (!newValue) {
				actionOutput.output(tuple);
				newValue = true;
			}
			newCount++;
		} else {
			dupCount++;
		}
	}

	@Override
	public void stopProcess(ActionContext context, ActionOutput actionOutput) throws Exception {
		spo.close();
		sop.close();
		ops.close();
		osp.close();
		pos.close();
		pso.close();
		context.incrCounter("Derived duplicates", dupCount);
		context.incrCounter("New Derivations", newCount);
	}
}

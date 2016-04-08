package uk.nhs.ciao.docs.transformer.eastkent;


import java.util.Map;

import uk.nhs.ciao.docs.parser.PropertyName;
import uk.nhs.ciao.docs.transformer.PropertiesTransformation;
import uk.nhs.ciao.docs.transformer.PropertyCloneUtils;
import uk.nhs.ciao.docs.transformer.PropertyMutator;
import uk.nhs.ciao.docs.transformer.TransformationRecorder;

import com.google.common.base.Preconditions;

/**
 * Takes a property and sets the output to a boolean that is true if the value matches the value specified
 */
public class ConditionalBooleanPropertyTransformation implements PropertiesTransformation {
	private final PropertyName from;
	private final PropertyMutator to;
	private final boolean retainOriginal;
	private final boolean cloneNestedProperties;
	private final String trueIfValueEquals;
	
	/**
	 * Takes a property and sets the output to a boolean that is true if the value matches the value specified. Original property is retained.
	 */
	public ConditionalBooleanPropertyTransformation(final PropertyName from, final PropertyMutator to, final String trueIfValueEquals) {
		this(from, to, trueIfValueEquals, true, false);
	}
	
	/**
	 * Creates a new property rename transformation
	 */
	public ConditionalBooleanPropertyTransformation(final PropertyName from, final PropertyMutator to, final String trueIfValueEquals,
			final boolean retainOriginal, final boolean cloneNestedProperties) {
		this.from = Preconditions.checkNotNull(from);
		this.to = Preconditions.checkNotNull(to);
		this.retainOriginal = retainOriginal;
		this.cloneNestedProperties = cloneNestedProperties;
		this.trueIfValueEquals = trueIfValueEquals;
	}
	
	@Override
	public void apply(final TransformationRecorder recorder, final Map<String, Object> source,
			final Map<String, Object> destination) {
		Object value = from.get(source);
		if (value == null) {
			return;
		} else if (!retainOriginal) {
			from.remove(source);
		}
		
		if (cloneNestedProperties) {
			value = PropertyCloneUtils.deepCloneNestedProperties(value);
		}
		
		// The result will be true only if the input exactly matches what was expected (ignoring case and trimming)
		Boolean result = (value.toString().trim().equalsIgnoreCase(this.trueIfValueEquals));

		to.set(recorder, from, destination, result);
	}
}
<%@ include file="../../common/includes/taglibs.jsp"%>
<c:set var="q" value="9" />
	<c:set var ="qpath" value="${surveypath}/${q}" />

			<tr>
				<td rowspan="2"><a href="${qpath}#9">9</a></td>
			</tr>
			<tr class="summaryRow">
				<td>9.1 <spring:message code="ref372"></spring:message></td>
				<td>1000 ha</td>
				<td class="numeric" colspan="5">${_fraVariable_9_1_1_}</td>
			</tr>
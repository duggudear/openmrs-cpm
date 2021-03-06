package org.openmrs.module.cpm.web.controller;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.cpm.ProposedConceptResponse;
import org.openmrs.module.cpm.ProposedConceptResponseDescription;
import org.openmrs.module.cpm.ProposedConceptResponseName;
import org.openmrs.module.cpm.ProposedConceptResponsePackage;
import org.openmrs.module.cpm.api.ProposedConceptService;
import org.openmrs.module.cpm.web.dto.concept.ConceptDto;
import org.openmrs.module.cpm.web.dto.SubmissionDto;
import org.openmrs.module.cpm.web.dto.SubmissionResponseDto;
import org.openmrs.module.cpm.web.dto.SubmissionStatusDto;
import org.openmrs.module.cpm.web.dto.concept.DescriptionDto;
import org.openmrs.module.cpm.web.dto.concept.NameDto;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
public class DictionaryManagerController {

	//
	// Proposer-Reviewer webservice endpoints
	//

    @RequestMapping(value = "/cpm/dictionarymanager/proposals", method = RequestMethod.POST)
    public @ResponseBody SubmissionResponseDto submitProposal(HttpServletRequest request, @RequestBody final SubmissionDto incomingProposal) throws IOException {

		final ProposedConceptService service = Context.getService(ProposedConceptService.class);
		final ProposedConceptResponsePackage proposedConceptResponsePackage = new ProposedConceptResponsePackage();
		proposedConceptResponsePackage.setName(incomingProposal.getName());
		proposedConceptResponsePackage.setEmail(incomingProposal.getEmail());
		proposedConceptResponsePackage.setDescription(incomingProposal.getDescription());
		proposedConceptResponsePackage.setProposedConceptPackageUuid("is-this-really-needed?");

		if (incomingProposal.getConcepts() != null) {
			for (ConceptDto concept : incomingProposal.getConcepts()) {
				ProposedConceptResponse response = new ProposedConceptResponse();

				List<ProposedConceptResponseName> names = new ArrayList<ProposedConceptResponseName>();
				for (NameDto nameDto: concept.getNames()) {
					ProposedConceptResponseName name = new ProposedConceptResponseName();
					name.setName(nameDto.getName());
					name.setType(nameDto.getType());
					name.setLocale(new Locale(nameDto.getLocale()));
					names.add(name);
				}
				response.setNames(names);

				List<ProposedConceptResponseDescription> descriptions = new ArrayList<ProposedConceptResponseDescription>();
				for (DescriptionDto descriptionDto: concept.getDescriptions()) {
					ProposedConceptResponseDescription description = new ProposedConceptResponseDescription();
					description.setDescription(descriptionDto.getDescription());
					description.setLocale(new Locale(descriptionDto.getLocale()));
					descriptions.add(description);
				}
				response.setDescriptions(descriptions);

				response.setProposedConceptUuid(concept.getUuid());
				proposedConceptResponsePackage.addProposedConcept(response);
			}
		}

		service.saveProposedConceptResponsePackage(proposedConceptResponsePackage);

		SubmissionResponseDto responseDto = new SubmissionResponseDto();
        responseDto.setId(proposedConceptResponsePackage.getId());
        return responseDto;
    }

	@RequestMapping(value = "/cpm/dictionarymanager/proposalstatus/{proposalId}", method = RequestMethod.GET)
	public @ResponseBody SubmissionStatusDto getSubmissionStatus(@PathVariable int proposalId) {

		final ProposedConceptService service = Context.getService(ProposedConceptService.class);
		final ProposedConceptResponsePackage aPackage = service.getProposedConceptResponsePackageById(proposalId);

		return new SubmissionStatusDto(aPackage.getStatus());
	}

	@ExceptionHandler(APIAuthenticationException.class)
	public void apiAuthenticationExceptionHandler(Exception e, HttpServletResponse response) {

		if (Context.isAuthenticated()) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		} else {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.addHeader("WWW-Authenticate", "Basic realm=\"OpenMRS\"");
		}
	}
}

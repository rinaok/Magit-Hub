var GET_PAGE_DATA = 1;
var GET_COMMIT_FILES = 2;
var GET_FILE_CONTENT = 3;
var ERROR_CODE = -1;
var SUCCESS_CODE = 0;

$(function() { // onload...do
    var data = "reqType="+ GET_PAGE_DATA;
    $.ajax({
        method: 'GET',
        url: "repo",
        dataType: 'json',
        data : data,
        timeout: 4000,
        error: function (e) {
            console.log(e.responseText);
        },
        success: function (r) {
            var branches = r.branches;
            var commits = r.commits;
            var wc = r.wcFiles;
            showBranchesData(branches);
            showCommits(commits);
            showWCFiles(wc);
        }
    })
});

function showBranchesData(branches){
    var branchesList = $('#branchesList');
    branchesList.empty();
    for (var i = 0 ; i < branches.length; i++) {
        var branch = branches[i];
        if(branch.isHead){
            branchesList.append("<li class='list-group-item list-group-item-primary'><h6>" + branch.name + "  ||  Head Branch</h6><p>" + branch.commitSha1 + "</p>" +
                "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></li>");
        }
        else{
            branchesList.append("<li class='list-group-item list-group-item-light'><h6>" + branch.name + "</h6><p>" + branch.commitSha1 + "</p>" +
                "<input id=\"deleteBranch\" type=\"submit\" class=\"btn btn-primary pull-right\" value=\"Delete\" style=\"float: right;\"></li>");
        }
    }
}

function showWCFiles(files){
    var wcFileList = $('#wcFileList');
    wcFileList.empty();
    for (var i = 0 ; i < files.length; i++) {
        var file = files[i];
        if (!file.type.localeCompare("FILE")) {
            wcFileList.append("<button type=\"button\" class='list-group-item list-group-item-action'" +
                " data-toggle=\"modal\" data-target=\"#fileContentModal\" id=" + file.sha1 + ">" +
                "<h6>" + file.name + "</h6><p>"
                + file.sha1 + "</p></button>");
        }
    }
}

function showCommitFiles(files) {
    var table = document.getElementById("commitFilesTable");
    $("#commitFilesTable").find("tbody").empty();
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    files.forEach(function(row){
            tableBody = tableBody + "<tr>";
            for (var j = 0; j < table.rows.length; j++){
                tableBody = tableBody + "<td><b>Name </b>" + row["name"] + "<br><b>Type </b>" +
                row["type"] + "<br><b>SHA1 </b>" + row["sha1"] + "<br><b>Modified By </b>" + row["lastModifier"] +
                    "<br><b>Modification Date </b>" + row["modificationDate"] + "</td>";
            }
            tableBody = tableBody + "</tr>";
        }
    );
    $('#commitFilesTable tbody').append(tableBody);
}


function showCommits(commits){
    var tableBody = "";
    tableBody = tableBody + "<tr>";
    var table = document.getElementById("commitsTable");

    // Create the data rows.
// Create the data rows.
    commits.forEach(function(row) {
        // Create a new row in the table for every element in the data array.
        tableBody = tableBody + "<tr style=\"cursor:pointer\">";

        for (var j = 0; j < table.rows.length; j++){
            tableBody = tableBody + "<td>" + row["sha1"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["message"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["creationDate"] + "</td>";
            tableBody = tableBody + "<td>" + row["commit"]["createdBy"] + "</td>";
            tableBody = tableBody + "<td>" + row["pointingBranches"] + "</td>";
        }

        tableBody = tableBody + "</tr>";
    });

    $('#commitsTable tbody').append(tableBody);

    for (var i = 1; i < table.rows.length; i++) {
        for (var j = 0; j < table.rows[i].cells.length; j++) {
            table.rows[i].onclick = function (e) {
                var commitSha1 = e.currentTarget.firstChild.innerText;
                var data = "reqType=" + GET_COMMIT_FILES + "&commitSha1=" + commitSha1;
                $.ajax({
                    method: 'GET',
                    data: data,
                    url: "repo",
                    processData: false, // Don't process the files
                    contentType: false, // Set content type to false as jQuery will tell the server its a query string request
                    timeout: 4000,
                    error: function (e) {
                        alert(e.responseText);
                    },
                    success: function (files) {
                        showCommitFiles(files);
                    }
                });
            }
        }
    }
}

$(document).ready(function(){
    $(document).on('click', '.list-group', function(e) {
        var sha1;
        var fileName;
        if(e.target.nextElementSibling != null) {
            sha1 = e.target.nextElementSibling.innerHTML;
            fileName = e.target.innerHTML;
        }
        else {
            sha1 = e.target.innerHTML;
            fileName = e.target.nextElementSibling.innerHTML;
        }
        var data = "reqType=" + GET_FILE_CONTENT + "&fileSha1=" + sha1;
        $.ajax({
            url: 'repo',
            type: 'GET',
            data: data,
            error: function (e) {
                alert(e.responseText);
            },
            success: function (response) {
                // Add response in Modal body
                document.getElementById("content").value = response;
                // Display Modal
                $('#fileContentModal').find('.modal-header h2').text(fileName);
                $('#fileContentModal').find('.pl-4').text(sha1);
                $('#fileContentModal').modal('show');
            }
        });
    });
});

$(document).on('click', '#editBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    $("#content").attr("readonly", false);
    document.getElementById('saveBtn').style.visibility = 'visible';
    document.getElementById('cancelBtn').style.visibility = 'visible';
    document.getElementById("editBtn").disabled = true;
});

$(document).on('show.bs.modal', '#fileContentModal', function (e) {
    $("#content").attr("readonly", true);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
});

$(document).on('click', '#cancelBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    $("#content").attr("readonly", false);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
});

$(document).on('click', '#saveBtn', function (event) {
    event.preventDefault();
    var $modal = $('#fileContentModal');
    $("#content").attr("readonly", true);
    document.getElementById('saveBtn').style.visibility = 'hidden';
    document.getElementById('cancelBtn').style.visibility = 'hidden';
    document.getElementById("editBtn").disabled = false;
    var fileData = "fileSha1=" + $('#fileContentModal').find('.pl-4').firstChild.textContent + "&content=" + $("#content").val();
    $.ajax({
        url: 'wc',
        type: 'POST',
        data: fileData,
        error: function (e) {
            showPopup(ERROR_CODE, e.responseText);
        },
        success: function (response) {
            showPopup(SUCCESS_CODE, text);
            $('#fileContentModal').modal('hide');
        }
    });
});

function showPopup(responseCode, text) {

}


